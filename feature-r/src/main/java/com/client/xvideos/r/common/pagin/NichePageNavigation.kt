package com.client.xvideos.r.common.pagin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Сервер считает pages по всей группе, но отклоняет любой page выше 100.
internal fun availableNichePageCount(reportedPages: Int): Int = reportedPages.coerceIn(1, 100)

internal data class NichePageState(
    val infiniteScroll: Boolean,
    val pageIndex: Int = 0,
    val pageCount: Int = 1,
)

internal class NichePageNavigation(infiniteScroll: Boolean) {
    private val mutableState = MutableStateFlow(NichePageState(infiniteScroll))
    val state = mutableState.asStateFlow()

    fun setInfiniteScroll(enabled: Boolean) {
        mutableState.update { it.copy(infiniteScroll = enabled) }
    }

    fun selectPage(index: Int) {
        mutableState.update {
            if (it.infiniteScroll) it else it.copy(pageIndex = index.coerceIn(0, it.pageCount - 1))
        }
    }

    fun resetForSort() {
        mutableState.update { it.copy(pageIndex = 0, pageCount = 1) }
    }

    fun updatePageCount(infiniteScroll: Boolean, count: Int) {
        mutableState.update {
            if (it.infiniteScroll != infiniteScroll) return@update it
            val pageCount = availableNichePageCount(count)
            it.copy(pageCount = pageCount, pageIndex = it.pageIndex.coerceAtMost(pageCount - 1))
        }
    }
}
