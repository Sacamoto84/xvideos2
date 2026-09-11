package com.client.xvideos.r

import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.URL1
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.model.sanitize
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.model.sanitizeOrNull
import com.client.xvideos.r.network.json.RJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Защитный слой моделей R.
 *
 * Сериализация через RJson (coerceInputValues, ignoreUnknownKeys) защищает
 * от некорректных значений в сети и на диске.
 *
 * Второй сюжет — дедупликация по `id` в [sanitizeGifsInfoList]. Дубль ключа
 * роняет LazyLayout с «Key ... was already used»; в этом проекте так падало
 * трижды в разных разделах.
 */
class RSanitizeTest {

    /* ---------- RJson и отсутствующие поля ---------- */

    @Test
    fun `GifsInfo без части полей разбирается без null`() {
        val item = RJson.decodeFromString<GifsInfo>("""{"id":"abc"}""")

        assertEquals("abc", item.id)
        assertNotNull(item.tags)
        assertNotNull(item.urls)
        assertNotNull(item.contentType)
        assertNotNull(item.userName)
    }

    @Test
    fun `UserInfo без части полей разбирается без null`() {
        val user = RJson.decodeFromString<UserInfo>("""{"name":"Кто-то"}""")

        assertNotNull(user.url)
        assertNotNull(user.username)
    }

    @Test
    fun `NichesInfo без части полей разбирается без null`() {
        val niche = RJson.decodeFromString<NichesInfo>("""{"id":"big-areolas"}""")

        assertEquals("big-areolas", niche.id)
        assertNotNull(niche.name)
    }

    /* ---------- URL1.sanitize ---------- */

    @Test
    fun `явный null в urls превращается в пустую строку`() {
        // Именно так выглядит ответ, где сервер прислал null вместо ссылки:
        // значения по умолчанию тут не спасают, их перекрывает явный null.
        val urls = RJson.decodeFromString<URL1>("""{"thumbnail":null,"sd":null}""")

        val safe = urls.sanitize()

        assertEquals("", safe.thumbnail)
        assertEquals("", safe.sd)
    }

    @Test
    fun `нормальные ссылки sanitize не портит`() {
        val urls = URL1(thumbnail = "https://cdn/t.jpg", sd = "https://cdn/v.mp4", hd = "https://cdn/hd.mp4")

        val safe = urls.sanitize()

        assertEquals("https://cdn/t.jpg", safe.thumbnail)
        assertEquals("https://cdn/v.mp4", safe.sd)
        assertEquals("https://cdn/hd.mp4", safe.hd)
    }

    /* ---------- GifsInfo.sanitizeOrNull ---------- */

    @Test
    fun `элемент без id отбраковывается`() {
        assertNull(GifsInfo(id = "").sanitizeOrNull())
        assertNull(GifsInfo(id = "   ").sanitizeOrNull())
    }

    @Test
    fun `null в полях заменяется значениями по умолчанию`() {
        val json = """{"id":"x1","contentType":null,"description":null,"userName":null,"tags":null,"urls":null}"""
        val raw = RJson.decodeFromString<GifsInfo>(json)

        val safe = raw.sanitizeOrNull()

        assertNotNull("элемент с id обязан выжить", safe)
        requireNotNull(safe)
        assertEquals("Solo Female", safe.contentType)
        assertEquals("Описание", safe.description)
        assertEquals("userName", safe.userName)
        assertEquals(emptyList<String>(), safe.tags)
        assertEquals(URL1(), safe.urls)
    }

    @Test
    fun `пустые теги выбрасываются`() {
        val raw = GifsInfo(id = "x1", tags = listOf("bdsm", "", "   ", "solo"))

        val safe = raw.sanitizeOrNull()

        requireNotNull(safe)
        assertEquals(listOf("bdsm", "solo"), safe.tags)
    }

    /* ---------- sanitizeGifsInfoList ---------- */

    @Test
    fun `список чистится от мусора и дублей`() {
        val list = listOf(
            GifsInfo(id = "a"),
            GifsInfo(id = ""),      // без id — выбрасывается
            GifsInfo(id = "b"),
            GifsInfo(id = "a"),     // дубль — выбрасывается
        )

        val safe = list.sanitizeGifsInfoList()

        assertEquals(listOf("a", "b"), safe.map { it.id })
    }

    @Test
    fun `дубли id не доживают до ключей LazyLayout`() {
        // Отдельным тестом, потому что это не косметика: одинаковый ключ роняет
        // LazyLayout с IllegalArgumentException прямо при прокрутке.
        val list = List(5) { GifsInfo(id = "same") }

        val ids = list.sanitizeGifsInfoList().map { it.id }

        assertEquals(1, ids.size)
        assertTrue(ids.distinct().size == ids.size)
    }

    @Test
    fun `null вместо списка даёт пустой список`() {
        assertEquals(emptyList<GifsInfo>(), null.sanitizeGifsInfoList())
    }
}
