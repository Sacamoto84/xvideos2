package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
internal fun rememberCanScrollToBottom(gridState: LazyGridState) = remember(gridState) {
    derivedStateOf {
        val layoutInfo = gridState.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val visible = layoutInfo.visibleItemsInfo
        if (totalItems <= 4 || visible.isEmpty()) {
            false
        } else {
            visible.maxOf { it.index } < totalItems - 1
        }
    }
}
