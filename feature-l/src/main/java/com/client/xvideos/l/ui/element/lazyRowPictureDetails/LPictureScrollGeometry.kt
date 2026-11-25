package com.client.xvideos.l.ui.element.lazyRowPictureDetails

import kotlin.math.roundToInt

internal data class LPictureScrollTarget(val index: Int, val offset: Int)

internal class LPictureScrollGeometry(
    val firstItemIndex: Int,
    private val itemTops: LongArray,
    private val contentHeight: Long,
) {
    fun maxScroll(viewportHeight: Int): Long = (contentHeight - viewportHeight).coerceAtLeast(0L)

    fun topOf(index: Int): Long? = when (index) {
        0 -> 0L
        else -> itemTops.getOrNull(index - firstItemIndex)
    }

    fun targetAt(position: Long): LPictureScrollTarget {
        if (position <= 0L || itemTops.isEmpty() || position < itemTops.first()) {
            return LPictureScrollTarget(0, position.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
        }

        val item = firstAtSameHeight(nextStartAfter(position) - 1)
        val offset = (position - itemTops[item]).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return LPictureScrollTarget(firstItemIndex + item, offset)
    }

    fun numberAt(position: Long): Int? {
        if (itemTops.isEmpty()) return null
        val next = nextStartAfter(position)
        if (next == 0) return 1
        val current = firstAtSameHeight(next - 1)
        if (next == itemTops.size) return current + 1

        // Spread the ordinals that share a row across the distance to the next row.
        val fraction = (position - itemTops[current]).toDouble() / (itemTops[next] - itemTops[current])
        return (current + 1 + fraction * (next - current)).roundToInt()
    }

    private fun nextStartAfter(position: Long): Int {
        var low = 0
        var high = itemTops.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (itemTops[middle] <= position) low = middle + 1 else high = middle
        }
        return low
    }

    private fun firstAtSameHeight(index: Int): Int {
        var item = index
        // Cards that start at the same height form one anchor across the columns.
        while (item > 0 && itemTops[item - 1] == itemTops[item]) item--
        return item
    }
}

internal fun calculateLPictureScrollGeometry(
    cellWidths: IntArray,
    spacing: Int,
    prefixHeight: Int,
    firstItemIndex: Int,
    minimumItemHeight: Int,
    itemCount: Int,
    aspectRatio: (Int) -> Float,
): LPictureScrollGeometry {
    val laneEnds = LongArray(cellWidths.size) { prefixHeight.toLong() }
    val itemTops = LongArray(itemCount)
    for (item in itemTops.indices) {
        var lane = 0
        for (candidate in 1 until laneEnds.size) {
            if (laneEnds[candidate] < laneEnds[lane]) lane = candidate
        }
        itemTops[item] = laneEnds[lane]
        val itemHeight = (cellWidths[lane] / aspectRatio(item)).roundToInt().coerceAtLeast(minimumItemHeight)
        laneEnds[lane] += itemHeight.toLong() + spacing
    }
    val contentHeight = if (itemCount == 0) prefixHeight.toLong() else laneEnds.max() - spacing
    return LPictureScrollGeometry(firstItemIndex, itemTops, contentHeight)
}
