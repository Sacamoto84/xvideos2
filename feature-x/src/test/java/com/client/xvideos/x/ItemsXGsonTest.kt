package com.client.xvideos.x

import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parserVideoPreviewFromImageUrl
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Регрессия на падение при открытии избранного X.
 *
 * Избранное лежит файлами и читается `gson.fromJson`. Gson не вызывает
 * конструктор Kotlin и не применяет значения по умолчанию, если у класса нет
 * конструктора без аргументов, — тогда отсутствующее в JSON поле остаётся
 * `null` вопреки объявленному типу `String`. Такой `null` уезжал в
 * [parserVideoPreviewFromImageUrl] с non-null параметром и ронял приложение.
 */
class ItemsXGsonTest {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    @Test
    fun `отсутствующие поля не превращаются в null`() {
        // Ровно то, что лежит в файле избранного: Gson не пишет null-поля,
        // поэтому запись с пустым превью теряет ключ.
        val json = """{"id":123,"title":"Видео","href":"/video123/"}"""

        val item = gson.fromJson(json, ItemsX::class.java)

        assertEquals(123L, item.id)
        assertEquals("Видео", item.title)
        // Главное: не null. До правки здесь падало обращение в композиции.
        assertNotNull(item.previewImage)
        assertEquals("", item.previewImage)
        assertEquals("", item.previewVideo)
        assertEquals("", item.channel)
        assertEquals("", item.duration)
    }

    @Test
    fun `явный null в json не роняет разбор превью`() {
        val json = """{"id":1,"title":"x","href":"/x/","previewImage":null}"""

        val item = gson.fromJson(json, ItemsX::class.java)

        // Явный null Gson кладёт в поле в обход типа — на этот случай парсер
        // принимает nullable и отвечает null, а не падает.
        //
        // Раньше ответом была строка "null". Её проверял один вызывающий из
        // трёх, поэтому она ложилась в ItemsX.previewVideo и возвращалась сюда
        // же на следующем экране. Подробности — в XParsersTest.
        assertNull(parserVideoPreviewFromImageUrl(item.previewImage))
    }

    @Test
    fun `парсер превью переживает null и пустую строку`() {
        assertNull(parserVideoPreviewFromImageUrl(null))
        assertNull(parserVideoPreviewFromImageUrl(""))
        assertNull(parserVideoPreviewFromImageUrl("   "))
        assertNull(parserVideoPreviewFromImageUrl("null"))
    }

    /**
     * Контрольный отрицательный пример: класс, у которого не все параметры имеют
     * значения по умолчанию. Конструктора без аргументов у него нет, Gson идёт
     * через Unsafe — и поле остаётся null вопреки типу `String`.
     *
     * Это ровно то состояние, в котором был [ItemsX] до правки. Тест держит
     * объяснение проверяемым: если поведение Gson изменится, он упадёт.
     */
    private data class WithoutAllDefaults(
        val id: Long,
        val preview: String = ""
    )

    @Test
    fun `без значений по умолчанию у всех полей Gson оставляет null`() {
        val item = gson.fromJson("""{"id":1}""", WithoutAllDefaults::class.java)

        @Suppress("SENSELESS_COMPARISON")
        val previewIsNull = item.preview == null
        assertEquals(true, previewIsNull)
    }

    @Test
    fun `у ItemsX есть конструктор без аргументов`() {
        // Именно его использует Gson, и только благодаря ему применяются
        // значения по умолчанию. Пропадёт хоть одно — конструктор исчезнет.
        val hasNoArgConstructor = ItemsX::class.java.constructors.any { it.parameterCount == 0 }
        assertEquals(true, hasNoArgConstructor)
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
