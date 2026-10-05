package com.client.xvideos.common.videoplayer.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlayerHostTest {

    private fun createHost(
        mediaUrl: String = "http://test.mp4",
        isPaused: Boolean = false,
        isMuted: Boolean = false,
    ): MediaPlayerHost = MediaPlayerHost(
        mediaUrl = mediaUrl,
        isPaused = isPaused,
        isMuted = isMuted,
    )

    @Test
    fun `seekTo with valid seconds updates seekToTime and currentTime`() {
        val host = createHost(mediaUrl = "http://test.mp4")
        host.seekTo(15.5f)

        assertEquals(15.5f, host.seekToTime)
        assertEquals(15.5f, host.currentTime, 0.001f)
        assertFalse(host.isSliding)
        host.dispose()
    }

    @Test
    fun `seekTo with negative or NaN seconds ignores invalid input`() {
        val host = createHost(mediaUrl = "http://test.mp4")
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

    @Test
    fun `updateCurrentTime sanitizes negative and NaN values`() {
        val host = createHost(mediaUrl = "http://test.mp4")
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
        val host = createHost(mediaUrl = "http://test.mp4")
        host.updateTotalTime(120)
        assertEquals(120, host.totalTime)

        host.updateTotalTime(-5)
        assertEquals(0, host.totalTime)
        host.dispose()
    }

    @Test
    fun `play pause and mute controls update state properly`() {
        val host = createHost(mediaUrl = "http://test.mp4", isPaused = true, isMuted = false)
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

    @Test
    fun `loadUrl меняет адрес, заголовки и настройку DRM`() {
        val host = createHost(mediaUrl = "https://example.com/first.m3u8")
        val headers = mapOf("Referer" to "https://example.com/")

        host.loadUrl("https://example.com/second.m3u8", headers = headers)

        assertEquals("https://example.com/second.m3u8", host.url)
        assertEquals(headers, host.headers)
        assertNull(host.drmConfig)
        host.dispose()
    }
}
