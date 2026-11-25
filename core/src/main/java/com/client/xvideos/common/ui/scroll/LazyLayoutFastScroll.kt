package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LazyListFastScroll(
    state: LazyListState,
    modifier: Modifier,
    itemCount: () -> Int,
    visibleRange: () -> Pair<Float, Float>,
    firstItemIndex: Int = 0,
    sessionKey: Any? = state,
    bottomClearance: Dp = 0.dp,
) {
    MediaFastScroll(
        modifier = modifier,
        sessionKey = sessionKey,
        canScrollBackward = { state.canScrollBackward },
        canScrollForward = { state.canScrollForward },
        isScrollInProgress = { state.isScrollInProgress },
        visibleRange = visibleRange,
        mediaRange = { firstItemIndex until firstItemIndex + itemCount() },
        topVisibleMediaNumber = { items ->
            val layout = state.layoutInfo
            layout.visibleItemsInfo.asSequence()
                .filter {
                    it.index in items && it.offset < layout.viewportEndOffset &&
                        it.offset + it.size > layout.viewportStartOffset
                }
                .minOfOrNull { it.index - items.first + 1 }
        },
        requestScrollToItem = { state.requestScrollToItem(it) },
        stopScroll = { state.stopScroll(MutatePriority.UserInput) },
        bottomClearance = bottomClearance,
    )
}

@Composable
fun LazyGridFastScroll(
    state: LazyGridState,
    modifier: Modifier,
    itemCount: () -> Int,
    visibleRange: () -> Pair<Float, Float>,
    firstItemIndex: Int = 0,
    sessionKey: Any? = state,
    bottomClearance: Dp = 0.dp,
) {
    MediaFastScroll(
        modifier = modifier,
        sessionKey = sessionKey,
        canScrollBackward = { state.canScrollBackward },
        canScrollForward = { state.canScrollForward },
        isScrollInProgress = { state.isScrollInProgress },
        visibleRange = visibleRange,
        mediaRange = { firstItemIndex until firstItemIndex + itemCount() },
        topVisibleMediaNumber = { items ->
            val layout = state.layoutInfo
            layout.visibleItemsInfo.asSequence()
                .filter {
                    it.index in items && it.offset.y < layout.viewportEndOffset &&
                        it.offset.y + it.size.height > layout.viewportStartOffset
                }
                .minOfOrNull { it.index - items.first + 1 }
        },
        requestScrollToItem = { state.requestScrollToItem(it) },
        stopScroll = { state.stopScroll(MutatePriority.UserInput) },
        bottomClearance = bottomClearance,
    )
}
