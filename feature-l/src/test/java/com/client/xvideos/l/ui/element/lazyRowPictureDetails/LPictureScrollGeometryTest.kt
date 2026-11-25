package com.client.xvideos.l.ui.element.lazyRowPictureDetails

import org.junit.Assert.assertEquals
import org.junit.Test

class LPictureScrollGeometryTest {
    @Test
    fun masonryPlacesEachCardInTheShortestLane() {
        val heights = intArrayOf(400, 100, 100, 200, 100)
        val geometry = calculateLPictureScrollGeometry(
            cellWidths = intArrayOf(120, 120),
            spacing = 10,
            prefixHeight = 60,
            firstItemIndex = 1,
            minimumItemHeight = 1,
            itemCount = heights.size,
        ) { 120f / heights[it] }

        assertEquals(listOf(60L, 60L, 170L, 280L, 470L), (1..5).map { geometry.topOf(it) })
        assertEquals(270L, geometry.maxScroll(300))
        assertEquals(LPictureScrollTarget(0, 32), geometry.targetAt(32))
        assertEquals(LPictureScrollTarget(1, 50), geometry.targetAt(110))
        assertEquals(LPictureScrollTarget(3, 65), geometry.targetAt(235))
    }

    @Test
    fun differentLaneWidthsAndMinimumCardHeightContributeToTheScrollDistance() {
        val geometry = calculateLPictureScrollGeometry(
            cellWidths = intArrayOf(101, 100),
            spacing = 1,
            prefixHeight = 0,
            firstItemIndex = 2,
            minimumItemHeight = 48,
            itemCount = 3,
        ) { if (it == 2) 10f else 1f }

        assertEquals(101L, geometry.topOf(4))
        assertEquals(49L, geometry.maxScroll(100))
        assertEquals(LPictureScrollTarget(4, 12), geometry.targetAt(113))
        assertEquals(LPictureScrollTarget(0, 0), geometry.targetAt(0))
    }
}
