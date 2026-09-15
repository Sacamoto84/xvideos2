package com.client.xvideos.common.videoplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3U8HelperTest {

    private val helper = M3U8Helper()

    @Test
    fun `parseM3U8Content correctly parses video qualities with relative and absolute URLs`() {
        val baseUrl = "https://cdn.example.com/hls/master.m3u8"
        val m3u8 = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
            360p.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2000000,RESOLUTION=1280x720
            /vod/720p.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080
            https://other-cdn.com/1080p.m3u8
        """.trimIndent()

        val data = helper.parseM3U8Content(m3u8, baseUrl)

        assertEquals(3, data.videoQualities.size)
        // 360p - relative
        assertEquals("https://cdn.example.com/hls/360p.m3u8", data.videoQualities[0].url)
        assertEquals("360p", data.videoQualities[0].resolution)
        // 720p - domain-absolute (/vod/720p.m3u8)
        assertEquals("https://cdn.example.com/vod/720p.m3u8", data.videoQualities[1].url)
        assertEquals("720p", data.videoQualities[1].resolution)
        // 1080p - full URL
        assertEquals("https://other-cdn.com/1080p.m3u8", data.videoQualities[2].url)
        assertEquals("1080p", data.videoQualities[2].resolution)
    }

    @Test
    fun `parseM3U8Content handles empty content safely`() {
        val data = helper.parseM3U8Content("", "https://example.com/stream.m3u8")
        assertTrue(data.videoQualities.isEmpty())
        assertTrue(data.audioTracks.isEmpty())
        assertTrue(data.subtitleTracks.isEmpty())
    }
}
