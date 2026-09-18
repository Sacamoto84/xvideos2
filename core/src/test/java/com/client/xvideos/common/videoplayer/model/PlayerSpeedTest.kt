package com.client.xvideos.common.videoplayer.model

import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSpeedTest {

    @Test
    fun `PlayerSpeed entries have correct speed values and display names`() {
        assertEquals(0.25f, PlayerSpeed.X0_25.speed)
        assertEquals("0.25x", PlayerSpeed.X0_25.displayName)

        assertEquals(0.5f, PlayerSpeed.X0_5.speed)
        assertEquals("0.5x", PlayerSpeed.X0_5.displayName)

        assertEquals(0.75f, PlayerSpeed.X0_75.speed)
        assertEquals("0.75x", PlayerSpeed.X0_75.displayName)

        assertEquals(1.0f, PlayerSpeed.X1.speed)
        assertEquals("1.0x", PlayerSpeed.X1.displayName)

        assertEquals(1.25f, PlayerSpeed.X1_25.speed)
        assertEquals("1.25x", PlayerSpeed.X1_25.displayName)

        assertEquals(1.5f, PlayerSpeed.X1_5.speed)
        assertEquals("1.5x", PlayerSpeed.X1_5.displayName)

        assertEquals(2.0f, PlayerSpeed.X2.speed)
        assertEquals("2.0x", PlayerSpeed.X2.displayName)
    }

    @Test
    fun `PlayerSpeed DEFAULT is X1`() {
        assertEquals(PlayerSpeed.X1, PlayerSpeed.DEFAULT)
    }

    @Test
    fun `MediaPlayerHost speed property updates current speed`() {
        val host = MediaPlayerHost(initialSpeed = PlayerSpeed.DEFAULT)
        assertEquals(PlayerSpeed.X1, host.speed)

        host.speed = PlayerSpeed.X1_5
        assertEquals(PlayerSpeed.X1_5, host.speed)

        host.speed = PlayerSpeed.X0_5
        assertEquals(PlayerSpeed.X0_5, host.speed)
    }
}
