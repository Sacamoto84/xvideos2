package com.client.xvideos.l

import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.screens.screenFullScreen.LFullScreenPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Хранилище списка картинок для полноэкранного просмотра. До появления ключей
 * это была одна глобальная переменная: два быстрых открытия подряд перетирали
 * список друг друга, и первый экран показывал чужой альбом.
 */
class LFullScreenPayloadTest {

    private fun picture(url: String) = PicsDetails(
        height = 100,
        width = 100,
        is_animated = false,
        url_to_original = url,
        url_to_video = null,
        album = "1",
        thumbnails = emptyList()
    )

    @Test
    fun `каждое открытие получает свой ключ и свой список`() {
        val first = listOf(picture("https://cdn/a.jpg"))
        val second = listOf(picture("https://cdn/b.jpg"), picture("https://cdn/c.jpg"))

        val firstKey = LFullScreenPayload.put(first)
        val secondKey = LFullScreenPayload.put(second)

        assertNotEquals(firstKey, secondKey)
        assertEquals(first, LFullScreenPayload.get(firstKey))
        assertEquals(second, LFullScreenPayload.get(secondKey))
    }

    @Test
    fun `неизвестный ключ даёт пустой список`() {
        assertEquals(emptyList<PicsDetails>(), LFullScreenPayload.get("нет такого ключа"))
        assertEquals(emptyList<PicsDetails>(), LFullScreenPayload.get(""))
    }

    @Test
    fun `хранилище не растёт без границ`() {
        val keys = (1..4).map { index ->
            LFullScreenPayload.put(listOf(picture("https://cdn/$index.jpg")))
        }

        // Самая старая запись вытеснена, свежие на месте.
        assertTrue(LFullScreenPayload.get(keys.first()).isEmpty())
        assertTrue(LFullScreenPayload.get(keys.last()).isNotEmpty())
    }
}
