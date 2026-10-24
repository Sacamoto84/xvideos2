package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveRoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XLiveRoomParserTest {

    @Test
    fun `открытая комната с адресом потока — эфир и поля шапки`() {
        val room = parseXLiveRoom(
            """
            {"room_status": "public", "hls_source": "https://edge.test/llhls.m3u8?token=t",
             "broadcaster_username": "room_a", "room_title": "hi #tag", "num_viewers": 562,
             "broadcaster_gender": "female", "start_timestamp": 1792821731,
             "satisfaction_score": {"percent": 97, "up_votes": 30, "down_votes": 1}, "chat_settings": {}}
            """.trimIndent()
        )

        assertEquals(
            XLiveRoom(
                status = "public",
                hlsUrl = "https://edge.test/llhls.m3u8?token=t",
                username = "room_a",
                title = "hi #tag",
                viewers = 562,
                gender = "female",
                startedAt = 1792821731L,
                ratingPercent = 97,
            ),
            room
        )
        assertTrue(room!!.isLive)
    }

    @Test
    fun `без полей шапки — значения по умолчанию`() {
        assertEquals(XLiveRoom("offline", ""), parseXLiveRoom("""{"room_status": "offline", "hls_source": ""}"""))
    }

    @Test
    fun `офлайн с пустым адресом — не эфир`() {
        assertFalse(parseXLiveRoom("""{"room_status": "offline", "hls_source": ""}""")!!.isLive)
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
    fun `рейтинг без процента — null`() {
        assertNull(parseXLiveRoom("""{"room_status": "public", "satisfaction_score": {}}""")!!.ratingPercent)
    }

    @Test
    fun `не объект или без room_status — null`() {
        assertNull(parseXLiveRoom("<html>"))
        assertNull(parseXLiveRoom("[]"))
        assertNull(parseXLiveRoom("""{"hls_source": "https://edge.test/a.m3u8"}"""))
        assertNull(parseXLiveRoom("""{"room_status": null}"""))
    }
}
