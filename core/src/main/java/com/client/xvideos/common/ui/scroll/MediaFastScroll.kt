package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.atom.rememberScrollButtonEffect
import com.client.xvideos.common.ui.composition.LocalScrollbarAlpha
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val HANDLE_HIT_SIZE = 48.dp
private const val HANDLE_HIDE_DELAY_MILLIS = 1_500L

private data class ActiveDrag(val range: IntRange, val callbacks: MediaFastScrollDrag)

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

private class MediaFastScrollController(
    val canScrollBackState: State<() -> Boolean>,
    val canScrollAheadState: State<() -> Boolean>,
    val scrollingState: State<() -> Boolean>,
    val currentFractionState: State<() -> Float>,
    val availableMediaState: State<() -> IntRange>,
    val startDragState: State<(IntRange) -> MediaFastScrollDrag>,
    val stopInertiaState: State<suspend () -> Unit>,
) {
    var activeDrag by mutableStateOf<ActiveDrag?>(null)
    var dragFraction by mutableFloatStateOf(0f)

    fun canScrollBack(): Boolean = canScrollBackState.value()
    fun canScrollAhead(): Boolean = canScrollAheadState.value()
    fun scrolling(): Boolean = scrollingState.value()
    fun currentFraction(): Float = currentFractionState.value()
    fun availableMedia(): IntRange = availableMediaState.value()
}

@Composable
private fun rememberMediaFastScrollState(
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

@Composable
private fun androidx.compose.foundation.layout.BoxScope.MediaFastScrollOverlay(
    state: MediaFastScrollController,
    travel: Dp,
    enabled: Boolean,
    hazeState: HazeState,
) {
    val travelPx = with(LocalDensity.current) { travel.toPx() }
    val effect = rememberScrollButtonEffect()
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset {
                val fraction = if (state.activeDrag == null) state.currentFraction() else state.dragFraction
                IntOffset(0, (fraction * travelPx).roundToInt())
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val drag = state.activeDrag
        val positionLabel = drag?.let { active ->
            val number by remember(active) { derivedStateOf { active.callbacks.positionNumber(state.dragFraction) } }
            number?.let { "$it / ${active.range.last - active.range.first + 1}" }
        }
        FastScrollPositionLabel(
            positionLabel = positionLabel,
            offsetY = {
                if (state.activeDrag == null) {
                    ((state.dragFraction - state.currentFraction()) * travelPx).roundToInt()
                } else {
                    0
                }
            },
            effect = effect,
            hazeState = hazeState,
        )
        FastScrollHandle(
            modifier = Modifier
                .size(HANDLE_HIT_SIZE)
                .semantics { contentDescription = "Быстрая прокрутка" }
                .fastScrollDragGesture(state, enabled, travelPx),
            effect = effect,
            hazeState = hazeState,
        )
    }
}

private fun Modifier.fastScrollDragGesture(
    state: MediaFastScrollController,
    enabled: Boolean,
    travelPx: Float,
): Modifier = pointerInput(travelPx) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown()
            val range = state.availableMedia()
            if (!enabled || range.isEmpty()) return@awaitEachGesture

            down.consume()
            val stop = launch(start = CoroutineStart.UNDISPATCHED) { state.stopInertiaState.value() }
            state.dragFraction = state.currentFraction()
            try {
                val callbacks = state.startDragState.value(range)
                state.activeDrag = ActiveDrag(range, callbacks)
                var pointer = down.id
                while (true) {
                    val change = awaitDragOrCancellation(pointer) ?: break
                    if (state.activeDrag == null || !state.availableMedia().preserves(range)) break
                    val next = (state.dragFraction + change.positionChange().y / travelPx).coerceIn(0f, 1f)
                    if (next != state.dragFraction) {
                        state.dragFraction = next
                        callbacks.moveTo(next)
                    }
                    change.consume()
                    if (!change.pressed) break
                    pointer = change.id
                }
            } finally {
                stop.cancel()
                state.activeDrag = null
            }
        }
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
