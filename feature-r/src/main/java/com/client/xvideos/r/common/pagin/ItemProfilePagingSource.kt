package com.client.xvideos.r.common.pagin

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.model.sanitizeGifsInfoList
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class ItemProfilePagingSource (val profileName : String, val sort : Order, val block: BlockRed, val redApi: RedApi, val tags : List<String> = emptyList()): PagingSource<Int, GifsInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int,  GifsInfo> {

        val page = params.key ?: 1 // API нумерует страницы с 1

        return try {
            Timber.d("!!! ItemProfilePagingSource::load() page = $page profileName:${profileName} sortTop:$sort")

//            if (sort == Order.FORCE_TEMP) {
//                LoadResult.Page(
//                    data = emptyList(),
//                    prevKey = null,
//                    nextKey = page
//                )
//            }

            val response = if (tags.isEmpty())
                redApi.searchCreator(userName = profileName, page = page,  count = 100, type = MediaType.GIF,  order = sort)
            else
                redApi.searchCreator(userName = profileName, page = page,  count = 100, type = MediaType.GIF,  order = sort , tags = tags)

            val responseBody = response.getOrThrow()
            val gifs : List<GifsInfo> = responseBody.gifs.sanitizeGifsInfoList()

            // G4: конец пагинации определяем по метаданным ответа (pages),
            // без лишнего «пустого» запроса в конце ленты.
            val nextKey = if (page < responseBody.pages) page + 1 else null

            Timber.d("!!! load() a.gif.size = ${gifs.size}")

            val blockedSet = block.blockList.value.map{it.id}.toSet()
            val gifs1 = gifs.filterNot { it.id in blockedSet }

            val user = responseBody.users.orEmpty().distinctBy { it.username }

            for (info in user) {
                UsersRed.addUser(info)
            }

            LoadResult.Page(
                data = gifs1,
                prevKey = null,
                nextKey = nextKey
            )

        } catch (e: CancellationException) {
            throw e // G1: отмена корутины не должна превращаться в LoadResult.Error
        } catch (e: Exception) {
            // G2: показ ошибки — ответственность UI (LoadState), а не data-слоя.
            Timber.e(e, "!!! ItemProfilePagingSource load() profileName:$profileName page = $page")
            LoadResult.Error(e)
        }
    }

    // G3: при refresh сохраняем позицию вокруг anchorPosition вместо рестарта с 1-й страницы.
    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? {
        return state.anchorPosition?.let { anchor ->
            val closest = state.closestPageToPosition(anchor)
            closest?.prevKey?.plus(1) ?: closest?.nextKey?.minus(1)
        }
    }
}
