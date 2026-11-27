package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
    gridState: LazyListState,
    itemsToIgnore: Int = 0,
): State<Pair<Float, Float>> {
    val result = remember { mutableStateOf(0f to 1f) }

    LaunchedEffect(gridState, itemsToIgnore) {
        snapshotFlow { gridState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                val safeIgnore = itemsToIgnore.coerceAtLeast(0)
                val visible = layoutInfo.visibleItemsInfo
                val first = visible.firstOrNull { it.index >= safeIgnore }
                val last = visible.lastOrNull()
                result.value = calculateVisibleRangePercent(
                    VisibleRangeMetrics(
                        totalItemsCount = layoutInfo.totalItemsCount,
                        itemsToIgnore = safeIgnore,
                        numberOfColumns = 1,
                        viewportStartOffset = layoutInfo.viewportStartOffset,
                        viewportEndOffset = layoutInfo.viewportEndOffset,
                        hasVisibleItems = visible.isNotEmpty(),
                        allVisibleIgnored = visible.all { it.index < safeIgnore },
                        firstRelevantIndex = first?.index,
                        firstItemTopEdge = first?.offset?.toFloat() ?: 0f,
                        firstItemHeight = first?.size?.toFloat() ?: 0f,
                        lastIndex = last?.index,
                        lastItemTopEdge = last?.offset?.toFloat() ?: 0f,
                        lastItemHeight = last?.size?.toFloat() ?: 0f,
                    )
                )
            }
    }
    return result
}
