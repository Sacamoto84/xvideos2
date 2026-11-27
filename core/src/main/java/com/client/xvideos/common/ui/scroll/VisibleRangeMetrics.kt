package com.client.xvideos.common.ui.scroll

internal data class VisibleRangeMetrics(
    val totalItemsCount: Int,
    val itemsToIgnore: Int,
    val numberOfColumns: Int,
    val viewportStartOffset: Int,
    val viewportEndOffset: Int,
    val hasVisibleItems: Boolean,
    val allVisibleIgnored: Boolean,
    val firstRelevantIndex: Int?,
    val firstItemTopEdge: Float,
    val firstItemHeight: Float,
    val lastIndex: Int?,
    val lastItemTopEdge: Float,
    val lastItemHeight: Float,
)
