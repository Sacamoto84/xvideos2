package com.client.xvideos.r.ui.fullscreen

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenRedFullScreenSMTest {

    @Test
    fun `sanitizePointTime clamps negative position to zero`() {
        assertEquals(0f, sanitizePointTime(-5f, 60))
        assertEquals(0f, sanitizePointTime(-0.1f, 0))
    }

    @Test
    fun `sanitizePointTime returns zero for non-finite values`() {
        assertEquals(0f, sanitizePointTime(Float.NaN, 60))
        assertEquals(0f, sanitizePointTime(Float.POSITIVE_INFINITY, 60))
        assertEquals(0f, sanitizePointTime(Float.NEGATIVE_INFINITY, 60))
    }

    @Test
    fun `sanitizePointTime clamps position to duration when duration is positive`() {
        assertEquals(45f, sanitizePointTime(45f, 60))
        assertEquals(60f, sanitizePointTime(75f, 60))
    }

    @Test
    fun `sanitizePointTime allows unbounded positive position when duration is zero or unknown`() {
        assertEquals(12.5f, sanitizePointTime(12.5f, 0))
        assertEquals(30f, sanitizePointTime(30f, -1))
    }

    @Test
    fun `FeedPlaybackState defaults to DEFAULT player speed`() {
        val state = com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedPlaybackState(
            timeA = 0f,
            timeB = 0f,
            enableAB = false,
            play = true,
            mute = false
        )
        assertEquals(com.client.xvideos.common.videoplayer.model.PlayerSpeed.DEFAULT, state.speed)
    }

    @Test
    fun `FeedPlaybackState accepts custom player speed`() {
        val state = com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedPlaybackState(
            timeA = 0f,
            timeB = 0f,
            enableAB = false,
            play = true,
            mute = false,
            speed = com.client.xvideos.common.videoplayer.model.PlayerSpeed.X1_5
        )
        assertEquals(com.client.xvideos.common.videoplayer.model.PlayerSpeed.X1_5, state.speed)
    }
}
