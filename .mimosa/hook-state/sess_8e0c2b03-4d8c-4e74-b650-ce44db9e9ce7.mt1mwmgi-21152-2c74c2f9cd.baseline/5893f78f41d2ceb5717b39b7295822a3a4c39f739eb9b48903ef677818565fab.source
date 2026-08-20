package com.client.xvideos.r.common.pagin

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.sanitizeGifsInfoList
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class ItemCollectionPagingSource(val collection: String?, val savedRed: SavedRed) : PagingSource<Int, GifsInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GifsInfo> {

        return try {
            Timber.d("!!! ItemCollectionPagingSource::load() collection:${collection}")

            val a = if (!collection.isNullOrEmpty()) {
                savedRed.collections.collectionList.first { it.collection == collection }.items
            } else
                emptyList()

            LoadResult.Page( data = a.sanitizeGifsInfoList(), prevKey = null, nextKey = null )

        } catch (e: CancellationException) {
            throw e // G1
        } catch (e: Exception) {
            // G2: без SnackBar из data-слоя; ошибку отрисует UI по LoadState.
            Timber.e(e, "!!! ItemCollectionPagingSource::load() collection:$collection")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? {
        return null
    }
}
