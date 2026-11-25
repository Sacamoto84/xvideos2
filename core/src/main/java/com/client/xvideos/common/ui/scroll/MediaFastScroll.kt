package com.client.xvideos.common.ui.scroll

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.rememberScrollButtonEffect
import com.client.xvideos.common.ui.atom.scrollFabSurface
import com.client.xvideos.common.ui.composition.LocalScrollbarAlpha
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val handleHitSize = 48.dp
private const val HANDLE_HIDE_DELAY_MILLIS = 1_500L
private const val POSITION_LABEL_FADE_MILLIS = 150

class MediaFastScrollDrag(
    val moveTo: (Float) -> Unit,
    val positionNumber: (Float) -> Int?,
)

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
    val canScrollBack by rememberUpdatedState(canScrollBackward)
    val canScrollAhead by rememberUpdatedState(canScrollForward)
    val scrolling by rememberUpdatedState(isScrollInProgress)
    val currentFraction by rememberUpdatedState(positionFraction)
    val availableMedia by rememberUpdatedState(mediaRange)
    val startDrag by rememberUpdatedState(beginDrag)
    val stopInertia by rememberUpdatedState(stopScroll)
    var activeDrag by remember { mutableStateOf<ActiveDrag?>(null) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var showHandle by remember { mutableStateOf(false) }

    val enabled by remember(pagerAlpha) {
        derivedStateOf { pagerAlpha() > 0f && (canScrollBack() || canScrollAhead()) && !availableMedia().isEmpty() }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { enabled && (scrolling() || activeDrag != null) }.collectLatest { active ->
            if (active) {
                showHandle = true
            } else if (showHandle) {
                delay(HANDLE_HIDE_DELAY_MILLIS)
                showHandle = false
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { activeDrag?.let { !availableMedia().preserves(it.range) } == true }
            .collect { invalidated -> if (invalidated) activeDrag = null }
    }

    val rail = modifier
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        .graphicsLayer {
            alpha = pagerAlpha()
            compositingStrategy = CompositingStrategy.ModulateAlpha
        }
    if (enabled && showHandle) BoxWithConstraints(rail) {
        val travel = (maxHeight - bottomClearance - handleHitSize).coerceAtLeast(0.dp)
        if (maxWidth < handleHitSize || travel == 0.dp) return@BoxWithConstraints
        val travelPx = with(LocalDensity.current) { travel.toPx() }
        val effect = rememberScrollButtonEffect()

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val fraction = if (activeDrag == null) currentFraction() else dragFraction
                    IntOffset(0, (fraction * travelPx).roundToInt())
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val drag = activeDrag
            val positionLabel = if (drag != null) {
                val number by remember(drag) { derivedStateOf { drag.callbacks.positionNumber(dragFraction) } }
                number?.let { "$it / ${drag.range.last - drag.range.first + 1}" }
            } else {
                null
            }
            var lastPositionLabel by remember { mutableStateOf<String?>(null) }
            if (positionLabel != null) {
                SideEffect { lastPositionLabel = positionLabel }
            }
            AnimatedVisibility(
                visible = positionLabel != null,
                modifier = Modifier.offset {
                    // Keep the fading label at the last drag position as the handle reanchors.
                    val offsetY = if (activeDrag == null) {
                        ((dragFraction - currentFraction()) * travelPx).roundToInt()
                    } else {
                        0
                    }
                    IntOffset(0, offsetY)
                },
                enter = fadeIn(tween(POSITION_LABEL_FADE_MILLIS)),
                exit = fadeOut(tween(POSITION_LABEL_FADE_MILLIS)),
                label = "scroll position",
            ) {
                val text = positionLabel ?: lastPositionLabel
                if (text != null) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .scrollFabSurface(effect = effect, hazeState = hazeState)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Theme.ScrollFab.contentColor,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
            FastScrollHandle(
                modifier = Modifier
                    .size(handleHitSize)
                    .semantics { contentDescription = "Быстрая прокрутка" }
                    .pointerInput(travelPx) {
                        coroutineScope {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val range = availableMedia()
                                if (!enabled || range.isEmpty()) return@awaitEachGesture

                                down.consume()
                                val stop = launch(start = CoroutineStart.UNDISPATCHED) { stopInertia() }
                                dragFraction = currentFraction()
                                try {
                                    val callbacks = startDrag(range)
                                    activeDrag = ActiveDrag(range, callbacks)
                                    var pointer = down.id
                                    while (true) {
                                        val change = awaitDragOrCancellation(pointer) ?: break
                                        val next = (dragFraction + change.positionChange().y / travelPx)
                                            .coerceIn(0f, 1f)
                                        if (activeDrag != null && availableMedia().preserves(range) && next != dragFraction) {
                                            dragFraction = next
                                            callbacks.moveTo(next)
                                        }
                                        change.consume()
                                        if (!change.pressed) break
                                        pointer = change.id
                                    }
                                } finally {
                                    stop.cancel()
                                    activeDrag = null
                                }
                            }
                        }
                    },
                effect = effect,
                hazeState = hazeState,
            )
        }
    }
}

@Composable
private fun FastScrollHandle(
    modifier: Modifier,
    effect: ScrollButtonEffect,
    hazeState: HazeState,
) {
    Box(modifier, contentAlignment = Alignment.CenterEnd) {
        Column(
            modifier = Modifier
                .padding(end = 2.dp)
                .size(width = 30.dp, height = 38.dp)
                .scrollFabSurface(effect = effect, hazeState = hazeState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val tint = Theme.ScrollFab.contentColor
            Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(16.dp), tint)
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp), tint)
        }
    }
}

private fun IntRange.preserves(frozen: IntRange): Boolean = first == frozen.first && last >= frozen.last

internal fun fastScrollTarget(fraction: Float, mediaRange: IntRange): Int? {
    if (mediaRange.isEmpty()) return null
    if (fraction <= 0f) return 0
    return mediaRange.first + ((mediaRange.last - mediaRange.first) * fraction.coerceAtMost(1f)).roundToInt()
}

internal fun fastScrollFraction(
    trackStart: Float,
    visibleSpan: Float,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
): Float = when {
    !canScrollBackward -> 0f
    !canScrollForward -> 1f
    visibleSpan >= 1f -> trackStart.coerceIn(0f, 1f)
    else -> (trackStart / (1f - visibleSpan)).coerceIn(0f, 1f)
}
