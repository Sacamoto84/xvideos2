package com.client.xvideos.r.ui.video

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedPooledVideoPlayerTest {

    @Test
    fun `clampSeekPositionMs clamps negative position to zero when duration is unset`() {
        assertEquals(0L, clampSeekPositionMs(-500L, C.TIME_UNSET))
    }

    @Test
    fun `clampSeekPositionMs preserves positive position when duration is unset`() {
        assertEquals(1500L, clampSeekPositionMs(1500L, C.TIME_UNSET))
    }

    @Test
    fun `clampSeekPositionMs clamps negative position to zero when duration is known`() {
        assertEquals(0L, clampSeekPositionMs(-100L, 5000L))
    }

    @Test
    fun `clampSeekPositionMs clamps position exceeding duration to duration`() {
        assertEquals(5000L, clampSeekPositionMs(6000L, 5000L))
    }

    @Test
    fun `clampSeekPositionMs preserves position within bounds`() {
        assertEquals(2500L, clampSeekPositionMs(2500L, 5000L))
    }

    @Test
    fun `clampSeekPositionMs handles exact boundaries correctly`() {
        assertEquals(0L, clampSeekPositionMs(0L, 5000L))
        assertEquals(5000L, clampSeekPositionMs(5000L, 5000L))
    }

    @Test
    fun `calculateDragDeltaMs returns zero for zero or non-finite drag`() {
        assertEquals(0L, calculateDragDeltaMs(0f))
        assertEquals(0L, calculateDragDeltaMs(Float.NaN))
        assertEquals(0L, calculateDragDeltaMs(Float.POSITIVE_INFINITY))
    }

    @Test
    fun `calculateDragDeltaMs returns one frame for small positive and negative drag`() {
        val frameMs = (1000f / 30f).toLong()
        assertEquals(frameMs, calculateDragDeltaMs(50f))
        assertEquals(-frameMs, calculateDragDeltaMs(-50f))
    }

    @Test
    fun `calculateDragDeltaMs returns one second for large drag exceeding threshold`() {
        assertEquals(1000L, calculateDragDeltaMs(450f))
        assertEquals(-1000L, calculateDragDeltaMs(-450f))
    }

    @Test
    fun `isValidABRange rejects NaN, infinite points, negative timeA, and inverted range`() {
        assertFalse(isValidABRange(enableAB = false, 1f, 5f))
        assertFalse(isValidABRange(enableAB = true, Float.NaN, 5f))
        assertFalse(isValidABRange(enableAB = true, 1f, Float.NaN))
        assertFalse(isValidABRange(enableAB = true, Float.NEGATIVE_INFINITY, 5f))
        assertFalse(isValidABRange(enableAB = true, 1f, Float.POSITIVE_INFINITY))
        assertFalse(isValidABRange(enableAB = true, -1f, 5f))
        assertFalse(isValidABRange(enableAB = true, -0.001f, 5f))
        assertFalse(isValidABRange(enableAB = true, 5f, 1f))
        assertFalse(isValidABRange(enableAB = true, 3f, 3f))
        assertTrue(isValidABRange(enableAB = true, 0f, 5f))
        assertTrue(isValidABRange(enableAB = true, 1f, 5f))
    }

    @Test
    fun `isZoomActive correctly detects active zoom state for pager and seek lock`() {
        assertFalse(com.client.xvideos.common.videoplayer.ui.isZoomActive(1.0f))
        assertFalse(com.client.xvideos.common.videoplayer.ui.isZoomActive(1.01f))
        assertTrue(com.client.xvideos.common.videoplayer.ui.isZoomActive(1.03f))
        assertTrue(com.client.xvideos.common.videoplayer.ui.isZoomActive(2.0f))
    }
}
