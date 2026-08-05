package com.client.xvideos.r.common.pagin

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import kotlinx.coroutines.CancellationException
import timber.log.Timber


class ItemSubscriptionsPagingSource (val savedRed: SavedRed): PagingSource<Int, GifsInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int,  GifsInfo> {

        return try {
            Timber.d("!!! >>>ItemLikesPagingSource::load()")
            val res = savedRed.subscriptions.refreshSubscription().sanitizeGifsInfoList().sortedByDescending{it.createDate}
            LoadResult.Page( data = res, prevKey = null,   nextKey = null )

        } catch (e: CancellationException) {
            throw e // G1
        } catch (e: Exception) {
            Timber.e(e, "!!! >>>ItemSubscriptionsPagingSource load()")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? {
        return null
    }
}
