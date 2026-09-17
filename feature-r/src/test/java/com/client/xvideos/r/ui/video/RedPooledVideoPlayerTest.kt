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
    fun `isFinite checks reject NaN and infinite points for AB loop`() {
        fun isValidAB(timeA: Float, timeB: Float) =
            timeA.isFinite() && timeB.isFinite() && timeB > timeA

        assertFalse(isValidAB(Float.NaN, 5f))
        assertFalse(isValidAB(1f, Float.NaN))
        assertFalse(isValidAB(Float.NEGATIVE_INFINITY, 5f))
        assertFalse(isValidAB(1f, Float.POSITIVE_INFINITY))
        assertFalse(isValidAB(5f, 1f))
        assertTrue(isValidAB(1f, 5f))
    }

    @Test
    fun `isZoomActive correctly detects active zoom state for pager and seek lock`() {
        assertFalse(com.client.xvideos.common.videoplayer.ui.isZoomActive(1.0f))
        assertFalse(com.client.xvideos.common.videoplayer.ui.isZoomActive(1.01f))
        assertTrue(com.client.xvideos.common.videoplayer.ui.isZoomActive(1.03f))
        assertTrue(com.client.xvideos.common.videoplayer.ui.isZoomActive(2.0f))
    }
}
