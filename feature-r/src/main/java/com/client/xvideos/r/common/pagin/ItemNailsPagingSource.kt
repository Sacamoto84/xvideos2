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

class ItemNailsPagingSource (val order : Order, val nichesName : String, val block: BlockRed, val redApi: RedApi): PagingSource<Int, GifsInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int,  GifsInfo> {

        val page = params.key ?: 1 // API нумерует страницы с 1

        return try {
            Timber.d("!!! ItemNailsPagingSource::load() page = $page sortTop:$order nichesName:$nichesName")

            if (order == Order.FORCE_TEMP) {
                LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = page
                )
            }

            val response = redApi.getNiches(niches = nichesName, page = page, order = order)

            val gifs : List<GifsInfo> = response.gifs.sanitizeGifsInfoList()

            // G4: конец пагинации по pages из ответа, без лишнего пустого запроса.
            val nextKey = if (page < response.pages) page + 1 else null

            Timber.d("!!! load() a.gif.size = ${gifs.size}")

            val blockedSet = block.blockList.value.map{it.id}.toSet()
            val gifs1 = gifs.filterNot { it.id in blockedSet }

            val user = response.users.orEmpty().distinctBy { it.username }

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
            Timber.e(e, "!!! ItemNailsPagingSource load() page = $page nichesName:$nichesName")
            LoadResult.Error(e)
        }
    }

    // G3
    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? {
        return state.anchorPosition?.let { position ->
            val closest = state.closestPageToPosition(position)
            closest?.prevKey?.plus(1) ?: closest?.nextKey?.minus(1)
        }
    }
}
