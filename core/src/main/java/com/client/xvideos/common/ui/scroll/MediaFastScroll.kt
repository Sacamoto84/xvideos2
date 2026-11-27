package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.composition.LocalScrollbarAlpha
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private val HANDLE_HIT_SIZE = 48.dp
private const val HANDLE_HIDE_DELAY_MILLIS = 1_500L

/**
 * A draggable thumb over a lazy layout. [mediaRange] excludes auxiliary rows.
 * [beginDrag] receives the range used for the duration of the gesture.
 */
@Composable
fun MediaFastScroll(
    modifier: Modifier,
    hazeState: HazeState,
    sessionKey: Any?,
    canScrollBackward: () -> Boolean,
    canScrollForward: () -> Boolean,
    isScrollInProgress: () -> Boolean,
    positionFraction: () -> Float,
    mediaRange: () -> IntRange,
    beginDrag: (IntRange) -> MediaFastScrollDrag,
    stopScroll: suspend () -> Unit,
    bottomClearance: Dp = 0.dp,
) = key(sessionKey) {
    val pagerAlpha = LocalScrollbarAlpha.current
    val state = rememberMediaFastScrollState(
        canScrollBackward, canScrollForward, isScrollInProgress,
        positionFraction, mediaRange, beginDrag, stopScroll, pagerAlpha,
    )
    val enabled by remember(pagerAlpha) {
        derivedStateOf {
            pagerAlpha() > 0f && (state.canScrollBack() || state.canScrollAhead()) && !state.availableMedia().isEmpty()
        }
    }
    var showHandle by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { enabled && (state.scrolling() || state.activeDrag != null) }.collectLatest { active ->
            if (active) {
                showHandle = true
            } else if (showHandle) {
                delay(HANDLE_HIDE_DELAY_MILLIS)
                showHandle = false
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { state.activeDrag?.let { !state.availableMedia().preserves(it.range) } == true }
            .collect { invalidated -> if (invalidated) state.activeDrag = null }
    }

    val rail = modifier
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        .graphicsLayer {
            alpha = pagerAlpha()
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }
    if (enabled && showHandle) BoxWithConstraints(rail) {
        val travel = (maxHeight - bottomClearance - HANDLE_HIT_SIZE).coerceAtLeast(0.dp)
        if (maxWidth < HANDLE_HIT_SIZE || travel == 0.dp) return@BoxWithConstraints
        MediaFastScrollOverlay(state = state, travel = travel, enabled = enabled, hazeState = hazeState)
    }
}

@Preview
@Composable
private fun MediaFastScrollPreview() {
    Box(Modifier.fillMaxSize()) {
        MediaFastScroll(
            modifier = Modifier.fillMaxSize(),
            hazeState = HazeState(),
            sessionKey = Unit,
            canScrollBackward = { true },
            canScrollForward = { true },
            isScrollInProgress = { false },
            positionFraction = { 0.4f },
            mediaRange = { 0..99 },
            beginDrag = { MediaFastScrollDrag(moveTo = {}, positionNumber = { 40 }) },
            stopScroll = {},
        )
    }
}
