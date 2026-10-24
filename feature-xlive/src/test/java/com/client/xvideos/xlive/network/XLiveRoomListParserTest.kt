package com.client.xvideos.xlive.network

import org.junit.Assert.assertEquals
import org.junit.Test

class XLiveRoomListParserTest {

    @Test
    fun `берутся открытые комнаты без пароля в порядке сайта`() {
        val json = """
            {"rooms": [
              {"username": "a", "current_show": "public", "has_password": false, "num_users": 9},
              {"username": "b", "current_show": "private", "has_password": false},
              {"username": "c", "current_show": "public", "has_password": true},
              {"username": "d", "current_show": "public"},
              {"username": "", "current_show": "public"},
              {"current_show": "public"}
            ], "total_count": 6}
        """.trimIndent()

        assertEquals(listOf("a", "d"), parseXLivePublicRooms(json))
    }

    @Test
    fun `не тот ответ — пустой список`() {
        assertEquals(emptyList<String>(), parseXLivePublicRooms("<html>"))
        assertEquals(emptyList<String>(), parseXLivePublicRooms("""{"total_count": 0}"""))
        assertEquals(emptyList<String>(), parseXLivePublicRooms("""{"rooms": {}}"""))
    }
}
