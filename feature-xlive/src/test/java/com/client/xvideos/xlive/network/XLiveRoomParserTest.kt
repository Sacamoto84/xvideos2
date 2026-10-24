package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveRoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XLiveRoomParserTest {

    @Test
    fun `открытая комната с адресом потока — эфир`() {
        val room = parseXLiveRoom(
            """{"room_status": "public", "hls_source": "https://edge.test/llhls.m3u8?token=t", "num_viewers": 5}"""
        )

        assertEquals(XLiveRoom("public", "https://edge.test/llhls.m3u8?token=t"), room)
        assertTrue(room!!.isLive)
    }

    @Test
    fun `офлайн с пустым адресом — не эфир`() {
        val room = parseXLiveRoom("""{"room_status": "offline", "hls_source": ""}""")

        assertEquals(XLiveRoom("offline", ""), room)
        assertFalse(room!!.isLive)
    }

    @Test
    fun `закрытое шоу с адресом — не эфир`() {
        assertFalse(parseXLiveRoom("""{"room_status": "private", "hls_source": "https://edge.test/a.m3u8"}""")!!.isLive)
    }

    @Test
    fun `hls_source null или без поля — пустой адрес`() {
        assertEquals("", parseXLiveRoom("""{"room_status": "away", "hls_source": null}""")!!.hlsUrl)
        assertEquals("", parseXLiveRoom("""{"room_status": "away"}""")!!.hlsUrl)
    }

    @Test
    fun `не объект или без room_status — null`() {
        assertNull(parseXLiveRoom("<html>"))
        assertNull(parseXLiveRoom("[]"))
        assertNull(parseXLiveRoom("""{"hls_source": "https://edge.test/a.m3u8"}"""))
        assertNull(parseXLiveRoom("""{"room_status": null}"""))
    }
}
