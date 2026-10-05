package com.client.xvideos.r.common.pagin

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.model.sanitizeGifsInfoList
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * [PagingSource] для главной ленты Explorer и поиска медиа RedGifs.
 *
 * Обрабатывает:
 * - Полнотекстовый поиск при непустом [searchText];
 * - Выбор соответствующего эндпоинта ленты при пустом [searchText] (неделя, месяц, всё время, тренды, свежие);
 * - Фильтрацию заблокированных материалов через [BlockRed];
 * - Пополнение глобального кэша авторов [UsersRed];
 * - Сохранение скролла вокруг anchorPosition при обновлении.
 *
 * @property sort Выбранный порядок сортировки [Order].
 * @property searchText Поисковая строка (если пустая — загружается стандартная лента).
 * @property block Фильтр заблокированных роликов.
 * @property redApi Сетевой клиент RedGifs.
 */
class ItemTopPagingSource(
    val sort: Order,
    val searchText: String,
    val block: BlockRed,
    val redApi: RedApi,
) : PagingSource<Int, GifsInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GifsInfo> {
        val page = params.key ?: 1

        return try {
            Timber.i("!!! ItemTopPagingSource::load() page = $page sortTop:$sort search:${searchText.isNotBlank()}")

            // getOrThrow один на все ветки: отказ сети обязан дойти сюда и стать
            // LoadResult.Error. Раньше кешируемые ленты возвращали голый
            // MediaResponse и на ошибке отдавали пустой объект — Paging видел
            // успешную пустую страницу, и вместо ошибки с кнопкой «повторить»
            // экран показывал пустоту. Через Result шла только LATEST, поэтому
            // одно и то же приложение вело себя по-разному в зависимости от
            // выбранной сортировки.
            val query = searchText.trim()
            val response = if (query.isNotBlank()) {
                redApi.search.searchGifs(query, sort, 100, page)
            } else {
                when (sort) {
                    Order.TOP_WEEK -> redApi.getTopThisWeek(100, page)
                    Order.TOP_MONTH -> redApi.getTopThisMonth(100, page)
                    Order.TOP -> redApi.getTopAllTime(100, page)
                    Order.TRENDING -> redApi.getTopTrending(100, page)
                    Order.LATEST -> redApi.getTopLatest(100, page)
                    // Сортировки без своей ленты. После сведения TOP_ALLTIME к
                    // TOP сюда попасть нечему из меню ленты, но откат оставлен:
                    // сортировка приходит параметром и набор может измениться.
                    // Громко — молчаливый откат прятал рассинхрон меню и запроса.
                    else -> {
                        Timber.w("!!! ItemTopPagingSource: у $sort нет своей ленты, отдаём неделю")
                        redApi.getTopThisWeek(100, page)
                    }
                }
            }.getOrThrow()

            val nextKey = if (page < response.pages) page + 1 else null

            val gifs: List<GifsInfo> = response.gifs.sanitizeGifsInfoList()
            Timber.d("!!! load() a.gif.size = ${gifs.size} page:$page pages:${response.pages}")

            val blockedSet = block.blockedIds.value
            val gifs1 = gifs.filterNot { it.id in blockedSet }

            val responseUsers = response.users
            val user = responseUsers.orEmpty().distinctBy { it.username }
            for (info in user) {
                UsersRed.addUser(info)
            }

            LoadResult.Page(
                data = gifs1,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: CancellationException) {
            throw e // G1
        } catch (e: Exception) {
            // G2: ошибку показывает UI через LoadState, без SnackBar из data-слоя.
            Timber.e("!!! ItemTopPagingSource load() page = $page: ${e.javaClass.simpleName}")
            LoadResult.Error(e)
        }
    }

    /**
     * Обновление всегда начинается с первой страницы.
     *
     * Источник отдаёт `prevKey = null`: страниц выше текущей для него нет.
     * Раньше отсюда возвращалась страница у якоря, и обновление не с верха
     * списка начало бы его со страницы N — всё, что выше, пропало бы.
     */
    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? = null
}
