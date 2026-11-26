package com.client.xvideos.common.ui.scroll

import kotlin.math.roundToInt

class MediaFastScrollDrag(
    val moveTo: (Float) -> Unit,
    val positionNumber: (Float) -> Int?,
)

internal fun indexedDrag(range: IntRange, requestScrollToItem: (Int) -> Unit): (Float) -> Unit {
    var lastTarget: Int? = null
    return { fraction ->
        val target = fastScrollTarget(fraction, range)
        if (target != null && target != lastTarget) {
            lastTarget = target
            requestScrollToItem(target)
        }
    }
}

internal fun IntRange.preserves(frozen: IntRange): Boolean = first == frozen.first && last >= frozen.last

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
