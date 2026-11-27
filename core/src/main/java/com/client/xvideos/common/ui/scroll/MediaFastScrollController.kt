package com.client.xvideos.common.ui.scroll

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal data class ActiveDrag(val range: IntRange, val callbacks: MediaFastScrollDrag)

internal class MediaFastScrollController(
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
