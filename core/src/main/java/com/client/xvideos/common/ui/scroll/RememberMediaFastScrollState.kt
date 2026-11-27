package com.client.xvideos.common.ui.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
internal fun rememberMediaFastScrollState(
    canScrollBackward: () -> Boolean,
    canScrollForward: () -> Boolean,
    isScrollInProgress: () -> Boolean,
    positionFraction: () -> Float,
    mediaRange: () -> IntRange,
    beginDrag: (IntRange) -> MediaFastScrollDrag,
    stopScroll: suspend () -> Unit,
    pagerAlpha: () -> Float,
): MediaFastScrollController {
    val canScrollBackState = rememberUpdatedState(canScrollBackward)
    val canScrollAheadState = rememberUpdatedState(canScrollForward)
    val scrollingState = rememberUpdatedState(isScrollInProgress)
    val currentFractionState = rememberUpdatedState(positionFraction)
    val availableMediaState = rememberUpdatedState(mediaRange)
    val startDragState = rememberUpdatedState(beginDrag)
    val stopInertiaState = rememberUpdatedState(stopScroll)
    return remember(pagerAlpha) {
        MediaFastScrollController(
            canScrollBackState, canScrollAheadState, scrollingState,
            currentFractionState, availableMediaState, startDragState, stopInertiaState,
        )
    }
}
