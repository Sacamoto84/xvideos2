package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveTag
import com.client.xvideos.xlive.model.XLiveTagPage
import com.client.xvideos.xlive.model.XLiveTagRoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XLiveTagParserTest {

    @Test
    fun `страница тегов — счётчики и превью комнат`() {
        val json = """
            {"hashtags": [
              {"hashtag": "bigboobs", "viewer_count": 249367, "room_count": 1195,
               "top_rooms": [{"room": "room_a", "img": "https://thumb.test/a.jpg", "viewers": 5},
                             {"room": "", "img": "https://thumb.test/x.jpg"}, {"img": "y"}]},
              {"hashtag": " asian ", "viewer_count": 2116, "room_count": 189},
              {"hashtag": "", "viewer_count": 1},
              {"viewer_count": 1}
            ], "total": 858}
        """.trimIndent()

        assertEquals(
            XLiveTagPage(
                tags = listOf(
                    XLiveTag("bigboobs", 249367, 1195, listOf(XLiveTagRoom("room_a", "https://thumb.test/a.jpg"))),
                    XLiveTag("asian", 2116, 189),
                ),
                total = 858,
            ),
            parseXLiveTagPage(json)
        )
    }

    @Test
    fun `ошибка параметров или не тот ответ — null`() {
        assertNull(parseXLiveTagPage("""{"errors": {"sort": ["Invalid sort field"]}}"""))
        assertNull(parseXLiveTagPage("<html>"))
        assertNull(parseXLiveTagPage("""{"hashtags": {}}"""))
    }
}
