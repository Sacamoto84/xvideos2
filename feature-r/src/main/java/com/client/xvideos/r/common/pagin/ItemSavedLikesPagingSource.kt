package com.client.xvideos.r.common.pagin

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.sanitizeGifsInfoList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber

/** Наблюдение за лайками живёт вместе с кэшируемым Pager, даже когда вкладка закрыта. */
internal fun savedLikesPagingFlow(
    likes: SnapshotStateList<GifsInfo>,
    config: PagingConfig,
    pagingSourceFactory: () -> PagingSource<Int, GifsInfo>,
): Flow<PagingData<GifsInfo>> = channelFlow {
    val factory = InvalidatingPagingSourceFactory(pagingSourceFactory)
    launch(start = CoroutineStart.UNDISPATCHED) {
        snapshotFlow { likes.toList() }.drop(1).collect { factory.invalidate() }
    }
    Pager(config, pagingSourceFactory = factory).flow.collect { send(it) }
}

/**
 * [PagingSource] для отображения сохраненных пользователем лайков гифок.
 *
 * Считывает снимок [likes], выполняет санитизацию и клиентскую сортировку согласно [order]
 * (по дате добавления, новизне или числу лайков).
 *
 * @property order Выбранный порядок сортировки.
 * @property likes Локальный список лайков.
 */
class ItemSavedLikesPagingSource(
    private val order: Order,
    private val likes: SnapshotStateList<GifsInfo>,
) : PagingSource<Int, GifsInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int,  GifsInfo> {
        return try {
            Timber.i("!!! >>>ItemSavedLikesPagingSource::load() sortTop:$order")
            val baseList = likes.toList().sanitizeGifsInfoList()
            val sortedList = when (order) {
                Order.OLDEST -> baseList.sortedBy { it.createDate }
                Order.TOP, Order.TOP_WEEK, Order.TOP_MONTH, Order.TOP28 -> baseList.sortedByDescending { it.likes }
                else -> baseList.sortedByDescending { it.createDate }
            }
            LoadResult.Page( data = sortedList, prevKey = null, nextKey = null )
        } catch (e: CancellationException) {
            throw e // G1
        } catch (e: Exception) {
            Timber.e(e, "!!! >>>ItemSavedLikesPagingSource load()")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, GifsInfo>): Int? { return null }
}
