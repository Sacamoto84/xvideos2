package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveRoomItem
import com.client.xvideos.xlive.model.XLiveRoomPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XLiveRoomListParserTest {

    @Test
    fun `страница целиком — поля карточки, промо, total строкой`() {
        val json = """
            {"rooms": [
              {"username": "room_a", "display_age": 21, "gender": "f", "country": "US", "num_users": 562,
               "room_subject": " goal #tag ", "tags": ["tag", ""], "img": "https://thumb.test/a.jpg?1",
               "is_new": true, "label": "new", "location": "United States"},
              {"username": "room_b", "display_age": 99, "gender": "c", "label": "promoted", "source_position": 1},
              {"username": "", "gender": "f"},
              {"gender": "m"}
            ], "total_count": "6718", "all_rooms_count": "7898"}
        """.trimIndent()

        assertEquals(
            XLiveRoomPage(
                rooms = listOf(
                    XLiveRoomItem(
                        username = "room_a",
                        age = 21,
                        gender = "f",
                        country = "US",
                        viewers = 562,
                        subject = "goal #tag",
                        tags = listOf("tag"),
                        thumbnail = "https://thumb.test/a.jpg?1",
                        isNew = true,
                    ),
                    XLiveRoomItem(username = "room_b", age = null, gender = "c", isPromoted = true),
                ),
                total = 6718,
            ),
            parseXLiveRoomPage(json)
        )
    }

    @Test
    fun `пустой список — страница без комнат`() {
        assertEquals(XLiveRoomPage(emptyList(), 0), parseXLiveRoomPage("""{"rooms": [], "total_count": "0"}"""))
    }

    @Test
    fun `ошибка параметров или не тот ответ — null`() {
        assertNull(parseXLiveRoomPage("""{"errors": {"limit": ["Ensure this value is less than or equal to 100."]}}"""))
        assertNull(parseXLiveRoomPage("<html>"))
        assertNull(parseXLiveRoomPage("""{"rooms": {}}"""))
    }
}
