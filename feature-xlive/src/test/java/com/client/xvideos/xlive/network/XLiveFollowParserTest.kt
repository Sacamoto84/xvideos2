package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveFollowedCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XLiveFollowParserTest {

    @Test
    fun `подписка из ответа`() {
        assertEquals(true, parseXLiveFollowing("""{"following": true, "notification_frequency": "every_online"}"""))
        assertEquals(false, parseXLiveFollowing("""{"following": false}"""))
    }

    @Test
    fun `без following или не JSON — null`() {
        assertNull(parseXLiveFollowing("""{"error": "x"}"""))
        assertNull(parseXLiveFollowing("<html></html>"))
    }

    @Test
    fun `счётчик подписок`() {
        val json = """{"online": 3, "total": 12, "online_rooms": [{"room": "room_a", "image": "https://img.test/a.jpg"}]}"""

        assertEquals(XLiveFollowedCount(online = 3, total = 12), parseXLiveFollowedCount(json))
    }

    @Test
    fun `счётчик без total или не JSON — null`() {
        assertNull(parseXLiveFollowedCount("""{"online": 3}"""))
        assertNull(parseXLiveFollowedCount("<html></html>"))
    }
}
