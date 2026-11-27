package com.client.xvideos.l.ui.element.lazyRowPictureDetails

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
internal fun rememberActiveItemRange(
    gridState: LazyStaggeredGridState,
    showInitialLoading: Boolean,
    columns: Int,
): State<IntRange> {
    // columns — ключ: derivedStateOf читает его как обычный параметр, без ключа
    // после раскрытия складного буферы остались бы от прежнего числа колонок.
    return remember(gridState, showInitialLoading, columns) {
        derivedStateOf {
            val visible = gridState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                0..15
            } else {
                val headerOffset = if (showInitialLoading) 2 else 1
                val first = (visible.minOf { it.index } - headerOffset).coerceAtLeast(0)
                val last = (visible.maxOf { it.index } - headerOffset).coerceAtLeast(0)
                val bufferBefore = columns
                val bufferAfter = columns * 2
                ((first - bufferBefore).coerceAtLeast(0))..(last + bufferAfter)
            }
        }
    }
}
