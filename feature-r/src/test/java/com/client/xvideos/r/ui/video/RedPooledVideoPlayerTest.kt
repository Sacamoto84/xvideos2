package com.client.xvideos.r.ui.video

import androidx.media3.common.C
import org.junit.Assert.assertEquals
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
}
