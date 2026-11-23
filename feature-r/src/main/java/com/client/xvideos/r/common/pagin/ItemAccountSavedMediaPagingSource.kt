package com.client.xvideos.r.common.pagin

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.network.api.RedApi

internal class ItemAccountSavedMediaPagingSource(
    private val redApi: RedApi,
    private val collectionId: String?,
) : PagingSource<Int, GifsInfo>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GifsInfo> {
        val page = params.key ?: 1
        return redApi.getAccountSavedMedia(collectionId, page, count = 30).fold(
            onSuccess = { response ->
                response.users.orEmpty().forEach { UsersRed.addUser(it) }
                LoadResult.Page(
                    data = response.gifs.sanitizeGifsInfoList(),
                    prevKey = null,
                    nextKey = if (page < response.pages && response.gifs.isNotEmpty()) page + 1 else null,
                )
            },
            onFailure = { LoadResult.Error(it) },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? = null
}
