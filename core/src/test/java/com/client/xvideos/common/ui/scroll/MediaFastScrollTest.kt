package com.client.xvideos.common.ui.scroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFastScrollTest {
    @Test
    fun dragReachesBothEndsAndTheMiddleOfALongAlbum() {
        assertEquals(0, fastScrollTarget(0f, 1..1000))
        assertEquals(501, fastScrollTarget(0.5f, 1..1000))
        assertEquals(1000, fastScrollTarget(1f, 1..1000))
    }

    @Test
    fun dragClampsToTheLastMediaItemAndIgnoresAnEmptyRange() {
        assertEquals(201, fastScrollTarget(1.2f, 2..201))
        assertNull(fastScrollTarget(0.5f, IntRange.EMPTY))
    }

    @Test
    fun handleUsesTheFullTrackEvenWhenSeveralItemsAreVisibleAtOnce() {
        assertEquals(0f, fastScrollFraction(0.2f, 0.2f, false, true), 0.0001f)
        assertEquals(0.5f, fastScrollFraction(0.4f, 0.2f, true, true), 0.0001f)
        assertEquals(1f, fastScrollFraction(0.7f, 0.2f, true, false), 0.0001f)
    }

    @Test
    fun rangePreservesAllowsTailGrowthButInvalidatesShrinkOrShift() {
        assertTrue((1..25).preserves(1..20))
        assertFalse((1..15).preserves(1..20))
        assertFalse((2..25).preserves(1..20))
        assertFalse(IntRange.EMPTY.preserves(1..20))
    }
}
