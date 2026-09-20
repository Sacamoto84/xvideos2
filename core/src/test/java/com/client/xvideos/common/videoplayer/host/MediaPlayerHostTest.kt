package com.client.xvideos.common.videoplayer.host

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaPlayerHostTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `seekTo with valid seconds updates seekToTime and currentTime`() {
        val host = MediaPlayerHost(mediaUrl = "http://test.mp4")
        host.seekTo(15.5f)

        assertEquals(15.5f, host.seekToTime)
        assertEquals(15.5f, host.currentTime, 0.001f)
        assertFalse(host.isSliding)
        host.dispose()
    }

    @Test
    fun `seekTo with negative or NaN seconds ignores invalid input`() {
        val host = MediaPlayerHost(mediaUrl = "http://test.mp4")
        host.seekTo(10f)
        assertEquals(10f, host.currentTime, 0.001f)

        host.seekTo(-5f)
        assertNull(host.seekToTime)
        assertEquals(10f, host.currentTime, 0.001f)

        host.seekTo(Float.NaN)
        assertNull(host.seekToTime)
        assertEquals(10f, host.currentTime, 0.001f)

        host.seekTo(Float.POSITIVE_INFINITY)
        assertNull(host.seekToTime)
        assertEquals(10f, host.currentTime, 0.001f)
        host.dispose()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `seekTo Int with negative seconds ignores invalid input`() {
        val host = MediaPlayerHost(mediaUrl = "http://test.mp4")
        host.seekTo(20)
        assertEquals(20f, host.currentTime, 0.001f)

        host.seekTo(-1)
        assertNull(host.seekToTime)
        assertEquals(20f, host.currentTime, 0.001f)
        host.dispose()
    }

    @Test
    fun `updateCurrentTime sanitizes negative and NaN values`() {
        val host = MediaPlayerHost(mediaUrl = "http://test.mp4")
        host.updateCurrentTime(42f)
        assertEquals(42f, host.currentTime, 0.001f)

        host.updateCurrentTime(-10f)
        assertEquals(0f, host.currentTime, 0.001f)

        host.updateCurrentTime(30f)
        assertEquals(30f, host.currentTime, 0.001f)

        host.updateCurrentTime(Float.NaN)
        assertEquals(0f, host.currentTime, 0.001f)
        host.dispose()
    }

    @Test
    fun `updateTotalTime sanitizes negative values`() {
        val host = MediaPlayerHost(mediaUrl = "http://test.mp4")
        host.updateTotalTime(120)
        assertEquals(120, host.totalTime)

        host.updateTotalTime(-5)
        assertEquals(0, host.totalTime)
        host.dispose()
    }

    @Test
    fun `play pause and mute controls update state properly`() {
        val host = MediaPlayerHost(mediaUrl = "http://test.mp4", isPaused = true, isMuted = false)
        assertTrue(host.isPaused)
        assertFalse(host.isMuted)

        host.play()
        assertFalse(host.isPaused)

        host.pause()
        assertTrue(host.isPaused)

        host.togglePlayPause()
        assertFalse(host.isPaused)

        host.mute()
        assertTrue(host.isMuted)
        assertEquals(0f, host.volumeLevel, 0.001f)

        host.unmute()
        assertFalse(host.isMuted)
        assertEquals(1f, host.volumeLevel, 0.001f)

        host.toggleMuteUnmute()
        assertTrue(host.isMuted)
        host.dispose()
    }
}
