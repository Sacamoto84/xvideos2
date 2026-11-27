package com.client.xvideos.common.ui.scroll

internal fun calculateVisibleRangePercent(metrics: VisibleRangeMetrics): Pair<Float, Float> {
    if (metrics.numberOfColumns <= 0 || !metrics.hasVisibleItems || metrics.totalItemsCount <= 0) return 0f to 1f

    val safeItemsToIgnore = metrics.itemsToIgnore.coerceAtLeast(0)
    val relevantTotalItemsCount = metrics.totalItemsCount - safeItemsToIgnore
    if (relevantTotalItemsCount <= 0) return 0f to 1f

    val totalRelevantRows = (relevantTotalItemsCount + metrics.numberOfColumns - 1) / metrics.numberOfColumns
    val firstRelevantIndex = metrics.firstRelevantIndex
    val lastIndex = metrics.lastIndex
    if (firstRelevantIndex == null || lastIndex == null || totalRelevantRows <= 0) {
        return when {
            metrics.allVisibleIgnored && relevantTotalItemsCount > 0 -> {
                val headFraction = (1f / (totalRelevantRows + 1)).coerceIn(0.01f, 0.5f)
                0f to headFraction
            }
            relevantTotalItemsCount > 0 -> 1f to 1f
            else -> 0f to 1f
        }
    }

    val firstRelevantRowIndex = (firstRelevantIndex - safeItemsToIgnore).coerceAtLeast(0) / metrics.numberOfColumns
    val fractionOfFirstItemHidden = if (metrics.firstItemHeight > 0f) {
        (-(metrics.firstItemTopEdge - metrics.viewportStartOffset) / metrics.firstItemHeight).coerceIn(0f, 1f)
    } else {
        0f
    }
    val startPercent = ((firstRelevantRowIndex + fractionOfFirstItemHidden) / totalRelevantRows)
        .coerceIn(0f, 1f)

    val lastRelevantRowIndex = (lastIndex - safeItemsToIgnore).coerceAtLeast(0) / metrics.numberOfColumns
    val visibleHeightOfLastItem = if (metrics.lastItemHeight > 0f) {
        (metrics.viewportEndOffset - metrics.lastItemTopEdge).coerceIn(0f, metrics.lastItemHeight)
    } else {
        0f
    }
    val fractionOfLastItemVisible = if (metrics.lastItemHeight > 0f) visibleHeightOfLastItem / metrics.lastItemHeight else 0f
    val endPercent = ((lastRelevantRowIndex + fractionOfLastItemVisible) / totalRelevantRows)
        .coerceIn(startPercent, 1f)

    return startPercent to endPercent
}
