package com.client.xvideos.l

import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.screens.screenFullScreen.LFullScreenPayload
import com.client.xvideos.l.ui.screens.screenFullScreen.resolveInitialIndex
import com.client.xvideos.l.ui.screens.screenFullScreen.resolveScrollIndex
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

    @Test
    fun `resolveInitialIndex находит точное совпадение`() {
        val items = listOf(picture("https://cdn/1.jpg"), picture("https://cdn/2.jpg"), picture("https://cdn/3.jpg"))
        assertEquals(1, resolveInitialIndex(items, items[1]))
    }

    @Test
    fun `resolveInitialIndex находит совпадение по selectionKey`() {
        val original = picture("https://cdn/target.jpg")
        val items = listOf(picture("https://cdn/1.jpg"), original, picture("https://cdn/3.jpg"))
        val targetVariant = original.copy(album = "999")
        assertEquals(1, resolveInitialIndex(items, targetVariant))
    }

    @Test
    fun `resolveInitialIndex возвращает 0 если ничего не найдено или список пуст`() {
        assertEquals(0, resolveInitialIndex(emptyList(), picture("https://cdn/none.jpg")))
        val items = listOf(picture("https://cdn/1.jpg"), picture("https://cdn/2.jpg"))
        assertEquals(0, resolveInitialIndex(items, picture("https://cdn/unknown.jpg")))
    }

    @Test
    fun `resolveScrollIndex сдвигает позицию на 2 назад и удерживает в границах списка`() {
        // При currentIndex = 0 или 1 результат не может быть меньше 0
        assertEquals(0, resolveScrollIndex(currentIndex = 0, maxIndex = 10))
        assertEquals(0, resolveScrollIndex(currentIndex = 1, maxIndex = 10))
        assertEquals(0, resolveScrollIndex(currentIndex = 2, maxIndex = 10))

        // При currentIndex >= 3 сдвигает на 2 назад для центрирования миниатюры
        assertEquals(3, resolveScrollIndex(currentIndex = 5, maxIndex = 10))
        assertEquals(8, resolveScrollIndex(currentIndex = 10, maxIndex = 10))

        // Превышение границ удерживается maxIndex
        assertEquals(10, resolveScrollIndex(currentIndex = 15, maxIndex = 10))

        // Пустой список (maxIndex = 0 или -1) возвращает 0 без сбоя
        assertEquals(0, resolveScrollIndex(currentIndex = 0, maxIndex = 0))
        assertEquals(0, resolveScrollIndex(currentIndex = 5, maxIndex = -1))
    }
}
