package com.client.xvideos.r.common.pagin

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.sanitizeGifsInfoList
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class ItemSavedLikesPagingSource (val order : Order, val savedRed: SavedRed): PagingSource<Int, GifsInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int,  GifsInfo> {
        return try {
            Timber.i("!!! >>>ItemSavedLikesPagingSource::load() sortTop:$order")
            LoadResult.Page( data = savedRed.likes.list.toList().sanitizeGifsInfoList(), prevKey = null, nextKey = null )
        } catch (e: CancellationException) {
            throw e // G1
        } catch (e: Exception) {
            Timber.e(e, "!!! >>>ItemSavedLikesPagingSource load()")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? { return null }
}
