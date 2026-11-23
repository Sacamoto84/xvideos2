package com.client.xvideos.r.common.pagin

import com.client.xvideos.r.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Для больших лент API может сообщить больше страниц, чем позволяет запросить.
internal fun availableFeedPageCount(reportedPages: Int): Int = reportedPages.coerceIn(1, 100)

internal data class FeedPageQuery(val order: Order, val tags: Set<String> = emptySet())

internal data class FeedPageState(
    val query: FeedPageQuery,
    val infiniteScroll: Boolean,
    val pageIndex: Int = 0,
    val pageCount: Int = 1,
)

internal class FeedPageNavigation(query: FeedPageQuery, infiniteScroll: Boolean) {
    private val mutableState = MutableStateFlow(FeedPageState(query, infiniteScroll))
    val state = mutableState.asStateFlow()

    fun setInfiniteScroll(enabled: Boolean) {
        mutableState.update { it.copy(infiniteScroll = enabled) }
    }

    fun selectPage(query: FeedPageQuery, index: Int) {
        mutableState.update {
            if (it.infiniteScroll || it.query != query) it else it.copy(pageIndex = index.coerceIn(0, it.pageCount - 1))
        }
    }

    fun updateQuery(query: FeedPageQuery) {
        mutableState.update { if (it.query == query) it else FeedPageState(query, it.infiniteScroll) }
    }

    fun updatePageCount(query: FeedPageQuery, infiniteScroll: Boolean, count: Int) {
        mutableState.update {
            if (it.infiniteScroll != infiniteScroll || it.query != query) return@update it
            val pageCount = availableFeedPageCount(count)
            it.copy(pageCount = pageCount, pageIndex = it.pageIndex.coerceAtMost(pageCount - 1))
        }
    }
}
