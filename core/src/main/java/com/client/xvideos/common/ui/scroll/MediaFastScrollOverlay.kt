package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val HANDLE_HIT_SIZE = 48.dp

@Composable
internal fun BoxScope.MediaFastScrollOverlay(
    state: MediaFastScrollController,
    travel: Dp,
    enabled: Boolean,
    hazeState: HazeState,
) {
    val travelPx = with(LocalDensity.current) { travel.toPx() }
    val currentTravelPx by rememberUpdatedState(travelPx)
    val currentEnabled by rememberUpdatedState(enabled)
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
                .fastScrollDragGesture(
                    state = state,
                    enabledProvider = { currentEnabled },
                    travelPxProvider = { currentTravelPx },
                ),
            effect = effect,
            hazeState = hazeState,
        )
    }
}

private fun Modifier.fastScrollDragGesture(
    state: MediaFastScrollController,
    enabledProvider: () -> Boolean,
    travelPxProvider: () -> Float,
): Modifier = pointerInput(state) {
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown()
            val range = state.availableMedia()
            val travelPx = travelPxProvider()
            if (!enabledProvider() || travelPx <= 0f || range.isEmpty()) return@awaitEachGesture

            down.consume()
            val stop = launch(start = CoroutineStart.UNDISPATCHED) { state.stopInertiaState.value() }
            state.dragFraction = state.currentFraction()
            try {
                val callbacks = state.startDragState.value(range)
                state.activeDrag = ActiveDrag(range, callbacks)
                var pointer = down.id
                while (true) {
                    val change = awaitDragOrCancellation(pointer) ?: break
                    val currentTravel = travelPxProvider()
                    if (currentTravel <= 0f || state.activeDrag == null || !state.availableMedia().preserves(range)) break
                    val next = (state.dragFraction + change.positionChange().y / currentTravel).coerceIn(0f, 1f)
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
