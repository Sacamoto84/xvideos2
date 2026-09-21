package com.client.xvideos.r.ui.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanvasTimeDurationLineTest {

    @Test
    fun `calculateSeekTime returns null when width is zero or negative`() {
        assertNull(calculateSeekTime(x = 50f, width = 0, duration = 100))
        assertNull(calculateSeekTime(x = 50f, width = -10, duration = 100))
    }

    @Test
    fun `calculateSeekTime returns null when duration is zero or negative`() {
        assertNull(calculateSeekTime(x = 50f, width = 200, duration = 0))
        assertNull(calculateSeekTime(x = 50f, width = 200, duration = -5))
    }

    @Test
    fun `calculateSeekTime clamps to zero when touch is before start`() {
        val seekTime = calculateSeekTime(x = -10f, width = 100, duration = 50)
        assertEquals(0f, seekTime ?: -1f, 0.001f)
    }

    @Test
    fun `calculateSeekTime clamps to duration when touch is past end`() {
        val seekTime = calculateSeekTime(x = 150f, width = 100, duration = 50)
        assertEquals(50f, seekTime ?: -1f, 0.001f)
    }

    @Test
    fun `calculateSeekTime calculates proportional time correctly`() {
        val seekTime = calculateSeekTime(x = 50f, width = 100, duration = 60)
        assertEquals(30f, seekTime ?: -1f, 0.001f)
    }

    @Test
    fun `calculateSeekTime returns null when x is NaN or infinite`() {
        assertNull(calculateSeekTime(x = Float.NaN, width = 100, duration = 60))
        assertNull(calculateSeekTime(x = Float.POSITIVE_INFINITY, width = 100, duration = 60))
        assertNull(calculateSeekTime(x = Float.NEGATIVE_INFINITY, width = 100, duration = 60))
    }

    @Test
    fun `calculateTimelineProgressWidth handles normal, out of bounds, and invalid values safely`() {
        assertEquals(500f, calculateTimelineProgressWidth(currentTime = 30f, duration = 60, canvasWidth = 1000f), 0.001f)
        assertEquals(0f, calculateTimelineProgressWidth(currentTime = -5f, duration = 60, canvasWidth = 1000f), 0.001f)
        assertEquals(1000f, calculateTimelineProgressWidth(currentTime = 120f, duration = 60, canvasWidth = 1000f), 0.001f)
        assertEquals(0f, calculateTimelineProgressWidth(currentTime = Float.NaN, duration = 60, canvasWidth = 1000f), 0.001f)
        assertEquals(0f, calculateTimelineProgressWidth(currentTime = Float.POSITIVE_INFINITY, duration = 60, canvasWidth = 1000f), 0.001f)
        assertEquals(0f, calculateTimelineProgressWidth(currentTime = 30f, duration = 0, canvasWidth = 1000f), 0.001f)
        assertEquals(0f, calculateTimelineProgressWidth(currentTime = 30f, duration = 60, canvasWidth = 0f), 0.001f)
    }

    @Test
    fun `calculateTimelinePointX handles normal, fallback on NaN and edge cases`() {
        assertEquals(250f, calculateTimelinePointX(pointTime = 15f, duration = 60, canvasWidth = 1000f), 0.001f)
        assertEquals(0f, calculateTimelinePointX(pointTime = Float.NaN, duration = 60, canvasWidth = 1000f, defaultRatio = 0f), 0.001f)
        assertEquals(1000f, calculateTimelinePointX(pointTime = Float.NaN, duration = 60, canvasWidth = 1000f, defaultRatio = 1f), 0.001f)
        assertEquals(0f, calculateTimelinePointX(pointTime = 15f, duration = -1, canvasWidth = 1000f), 0.001f)
        assertEquals(0f, calculateTimelinePointX(pointTime = 15f, duration = 60, canvasWidth = -10f), 0.001f)
    }
}
