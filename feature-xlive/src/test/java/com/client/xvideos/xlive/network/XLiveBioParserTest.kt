package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveBio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XLiveBioParserTest {

    @Test
    fun `анкета целиком`() {
        val bio = parseXLiveBio(
            """
            {"real_name": " Anna ", "display_age": 18, "location": "My Bedroom", "languages": "English",
             "body_type": "Petite", "interested_in": ["Women", "Men", ""], "follower_count": 20332,
             "about_me": "<p>Hello<br/>world</p>", "photo_sets": []}
            """.trimIndent()
        )

        assertEquals(
            XLiveBio(
                realName = "Anna",
                age = 18,
                location = "My Bedroom",
                languages = "English",
                bodyType = "Petite",
                interestedIn = listOf("Women", "Men"),
                followers = 20332,
                aboutMe = "Hello\nworld",
            ),
            bio
        )
    }

    @Test
    fun `запятая на краю списка в поле убирается`() {
        assertEquals("Slim, Fit", parseXLiveBio("""{"body_type": "Slim, Fit, "}""")!!.bodyType)
    }

    @Test
    fun `пустая анкета — значения по умолчанию`() {
        assertEquals(XLiveBio(), parseXLiveBio("""{"display_age": null, "interested_in": null}"""))
    }

    @Test
    fun `не объект — null`() {
        assertNull(parseXLiveBio("<html>"))
        assertNull(parseXLiveBio("[]"))
    }

    @Test
    fun `о себе — ссылки своим текстом, сущности раскрыты, лишние пустые строки сжаты`() {
        val html = "<p>Cum see more <br/><a href=\"/external_link/?url=x\">my page</a></p>" +
            "<p>&amp; more</p><br><br><br><p>bye</p>"

        assertEquals("Cum see more\nmy page\n& more\n\nbye", htmlToText(html))
    }

    @Test
    fun `о себе — пусто`() {
        assertEquals("", htmlToText(""))
        assertEquals("", htmlToText("<p> </p>"))
    }
}
