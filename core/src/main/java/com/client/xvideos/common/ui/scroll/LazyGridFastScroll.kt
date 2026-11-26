package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

@Composable
fun LazyGridFastScroll(
    state: LazyGridState,
    modifier: Modifier,
    hazeState: HazeState,
    itemCount: () -> Int,
    visibleRange: () -> Pair<Float, Float>,
    firstItemIndex: Int = 0,
    sessionKey: Any? = state,
    bottomClearance: Dp = 0.dp,
) {
    MediaFastScroll(
        modifier = modifier,
        hazeState = hazeState,
        sessionKey = sessionKey,
        canScrollBackward = { state.canScrollBackward },
        canScrollForward = { state.canScrollForward },
        isScrollInProgress = { state.isScrollInProgress },
        positionFraction = {
            val (start, end) = visibleRange()
            fastScrollFraction(start, end - start, state.canScrollBackward, state.canScrollForward)
        },
        mediaRange = { firstItemIndex until firstItemIndex + itemCount() },
        beginDrag = { items ->
            MediaFastScrollDrag(
                moveTo = indexedDrag(items) { state.requestScrollToItem(it) },
                positionNumber = {
                    val layout = state.layoutInfo
                    layout.visibleItemsInfo.asSequence()
                        .filter {
                            it.index in items && it.offset.y < layout.viewportEndOffset &&
                                it.offset.y + it.size.height > layout.viewportStartOffset
                        }
                        .minOfOrNull { it.index - items.first + 1 }
                },
            )
        },
        stopScroll = { state.stopScroll(MutatePriority.UserInput) },
        bottomClearance = bottomClearance,
    )
}

@Preview
@Composable
private fun LazyGridFastScrollPreview() {
    LazyGridFastScroll(
        state = rememberLazyGridState(),
        modifier = Modifier,
        hazeState = HazeState(),
        itemCount = { 0 },
        visibleRange = { 0f to 1f },
    )
}
