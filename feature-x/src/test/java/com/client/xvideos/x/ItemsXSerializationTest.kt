package com.client.xvideos.x

import com.client.xvideos.common.json.AppJson
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parserVideoPreviewFromImageUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тест сериализации и обратной совместимости для ItemsX.
 *
 * Проверяет, что файлы избранного и загрузок корректно разбираются
 * через kotlinx.serialization даже при неполных JSON-файлах, оставшихся
 * от прежних версий.
 */
class ItemsXSerializationTest {

    private val json = AppJson

    @Test
    fun `отсутствующие поля не превращаются в null`() {
        val raw = """{"id":123,"title":"Видео","href":"/video123/"}"""

        val item = json.decodeFromString<ItemsX>(raw)

        assertEquals(123L, item.id)
        assertEquals("Видео", item.title)
        assertNotNull(item.previewImage)
        assertEquals("", item.previewImage)
        assertEquals("", item.previewVideo)
        assertEquals("", item.channel)
        assertEquals("", item.duration)
    }

    @Test
    fun `явный null в json не роняет разбор превью`() {
        val raw = """{"id":1,"title":"x","href":"/x/","previewImage":null}"""

        val item = json.decodeFromString<ItemsX>(raw)

        assertNull(parserVideoPreviewFromImageUrl(item.previewImage))
    }

    @Test
    fun `парсер превью переживает null и пустую строку`() {
        assertNull(parserVideoPreviewFromImageUrl(null))
        assertNull(parserVideoPreviewFromImageUrl(""))
        assertNull(parserVideoPreviewFromImageUrl("   "))
        assertNull(parserVideoPreviewFromImageUrl("null"))
    }

    @Test
    fun `из ссылки на превью-картинку получается ссылка на видео`() {
        val image =
            "https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg"

        assertEquals(
            "https://cdn77-pic.xvideos-cdn.com/videos/videopreview/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad_169.mp4",
            parserVideoPreviewFromImageUrl(image)
        )
    }
}
