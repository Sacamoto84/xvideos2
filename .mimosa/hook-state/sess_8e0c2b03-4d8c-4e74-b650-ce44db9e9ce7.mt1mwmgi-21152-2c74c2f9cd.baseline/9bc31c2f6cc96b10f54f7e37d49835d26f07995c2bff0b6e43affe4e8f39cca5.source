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

class ItemTopPagingSource(
    val sort: Order,
    val searchText: String,
    val block: BlockRed,
    val redApi: RedApi,
) : PagingSource<Int, GifsInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GifsInfo> {
        val page = params.key ?: 1

        return try {
            Timber.i("!!! ItemTopPagingSource::load() page = $page sortTop:$sort searchText:$searchText")

            // getOrThrow один на все ветки: отказ сети обязан дойти сюда и стать
            // LoadResult.Error. Раньше кешируемые ленты возвращали голый
            // MediaResponse и на ошибке отдавали пустой объект — Paging видел
            // успешную пустую страницу, и вместо ошибки с кнопкой «повторить»
            // экран показывал пустоту. Через Result шла только LATEST, поэтому
            // одно и то же приложение вело себя по-разному в зависимости от
            // выбранной сортировки.
            val response = if (searchText.isNotEmpty()) {
                Timber.i("!!! ItemTopPagingSource::load()  RedGifs.searchGifs($searchText)")
                redApi.search.searchGifs(searchText, sort, 100, page)
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

            val blockedSet = block.blockList.value.map { it.id }.toSet()
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
            Timber.e(e, "!!! ItemTopPagingSource load() page = $page")
            LoadResult.Error(e)
        }
    }

    // G3
    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? {
        return state.anchorPosition?.let { anchor ->
            val closest = state.closestPageToPosition(anchor)
            closest?.prevKey?.plus(1) ?: closest?.nextKey?.minus(1)
        }
    }
}
