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
}
