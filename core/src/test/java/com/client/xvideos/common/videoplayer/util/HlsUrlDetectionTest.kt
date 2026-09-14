package com.client.xvideos.common.videoplayer.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsUrlDetectionTest {

    @Test
    fun `isHlsUrl returns true for standard m3u8 url`() {
        assertTrue(isHlsUrl("https://example.com/video/master.m3u8"))
        assertTrue(isHlsUrl("http://cdn.site.com/hls/stream.m3u8"))
    }

    @Test
    fun `isHlsUrl returns true for m3u8 url with query parameters`() {
        assertTrue(isHlsUrl("https://example.com/master.m3u8?token=abc123xyz&expires=1726000000"))
        assertTrue(isHlsUrl("https://cdn.example.com/stream.m3u8?key=val&other=123"))
    }

    @Test
    fun `isHlsUrl returns true for m3u8 url with fragment`() {
        assertTrue(isHlsUrl("https://example.com/master.m3u8#track-1"))
    }

    @Test
    fun `isHlsUrl returns true for m3u8 url with query and fragment`() {
        assertTrue(isHlsUrl("https://example.com/master.m3u8?token=abc#track-1"))
    }

    @Test
    fun `isHlsUrl is case insensitive`() {
        assertTrue(isHlsUrl("https://example.com/STREAM.M3U8"))
        assertTrue(isHlsUrl("https://example.com/Stream.M3U8?token=123"))
    }

    @Test
    fun `isHlsUrl returns false for progressive video formats`() {
        assertFalse(isHlsUrl("https://example.com/video.mp4"))
        assertFalse(isHlsUrl("https://example.com/video.mp4?token=abc"))
        assertFalse(isHlsUrl("https://example.com/video.webm"))
        assertFalse(isHlsUrl("https://example.com/video.mkv"))
    }

    @Test
    fun `isHlsUrl returns false for null or blank urls`() {
        assertFalse(isHlsUrl(null))
        assertFalse(isHlsUrl(""))
        assertFalse(isHlsUrl("   "))
    }
}
