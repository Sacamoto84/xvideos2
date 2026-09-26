package com.client.xvideos.x

import org.junit.Assert.assertEquals
import org.junit.Test

class XSiteTest {

    @Test
    fun `пустые и пробельные строки возвращают пустую строку`() {
        assertEquals("", normalizeXUrl(""))
        assertEquals("", normalizeXUrl("   "))
        assertEquals("", normalizeXUrl("\t\n"))
    }

    @Test
    fun `абсолютные ссылки с http и https сохраняются без изменений`() {
        assertEquals("https://www.xv-ru.com/video123", normalizeXUrl("https://www.xv-ru.com/video123"))
        assertEquals("http://example.com/stream.m3u8", normalizeXUrl("http://example.com/stream.m3u8"))
        assertEquals("https://cdn.xv-ru.com/hls.m3u8?token=abc", normalizeXUrl("https://cdn.xv-ru.com/hls.m3u8?token=abc"))
    }

    @Test
    fun `относительные ссылки с ведущим слэшем дополняются urlStart`() {
        assertEquals("$urlStart/video123", normalizeXUrl("/video123"))
        assertEquals("$urlStart/tags/blonde/1", normalizeXUrl("/tags/blonde/1"))
    }

    @Test
    fun `относительные ссылки без ведущего слэша корректно форматируются со слэшем`() {
        assertEquals("$urlStart/video123", normalizeXUrl("video123"))
        assertEquals("$urlStart/prof-video-click/456", normalizeXUrl("prof-video-click/456"))
    }

    @Test
    fun `пробелы по краям удаляются при нормализации`() {
        assertEquals("$urlStart/video123", normalizeXUrl("  /video123  "))
        assertEquals("https://example.com/video", normalizeXUrl("  https://example.com/video  "))
    }

    @Test
    fun `протокольно-относительные ссылки с двумя слэшами нормализуются в https`() {
        assertEquals("https://cdn.xv-ru.com/video.mp4", normalizeXUrl("//cdn.xv-ru.com/video.mp4"))
        assertEquals("https://example.com/stream.m3u8", normalizeXUrl("  //example.com/stream.m3u8  "))
    }

    @Test
    fun `extractXVideoId корректно извлекает числовые и буквенные id`() {
        assertEquals(12345L, extractXVideoId("/video12345/test"))
        assertEquals(789L, extractXVideoId("https://www.xv-ru.com/video.789/title"))

        // Буквенно-цифровые токены дают стабильный положительный ID
        val id1 = extractXVideoId("https://www.xv-ru.com/video.uicfdab07bd/_")
        val id2 = extractXVideoId("/video.uicfdab07bd/other_title")
        org.junit.Assert.assertNotNull(id1)
        org.junit.Assert.assertTrue(id1!! > 0L)
        assertEquals(id1, id2) // Детерминированность
    }

    @Test
    fun `parseDurationToMs корректно парсит различные форматы длительности`() {
        assertEquals(600_000L, parseDurationToMs("10 мин."))
        assertEquals(720_000L, parseDurationToMs("12 min"))
        assertEquals(4_500_000L, parseDurationToMs("1 hr 15 min"))
        assertEquals(45_000L, parseDurationToMs("45 sec"))
        assertEquals(754_000L, parseDurationToMs("12:34"))
        assertEquals(3_665_000L, parseDurationToMs("01:01:05"))
        assertEquals(0L, parseDurationToMs(""))
        assertEquals(0L, parseDurationToMs("   "))
        assertEquals(0L, parseDurationToMs("No duration"))
    }

    @Test
    fun `toNormalizedXUrl extension matches top-level function`() {
        assertEquals("/video123".toNormalizedXUrl(), normalizeXUrl("/video123"))
        assertEquals("".toNormalizedXUrl(), "")
    }

    @Test
    fun `isXVideoUrl and isValidXUrl validate urls properly`() {
        assertEquals(true, isXVideoUrl("/video12345/test"))
        assertEquals(true, isXVideoUrl("https://www.xv-ru.com/video.789/title"))
        assertEquals(false, isXVideoUrl("/tags/blonde/1"))
        assertEquals(false, isXVideoUrl(""))

        assertEquals(true, isValidXUrl("https://example.com/video"))
        assertEquals(true, isValidXUrl("http://example.com/video"))
        assertEquals(true, isValidXUrl("/video123"))
        assertEquals(true, isValidXUrl("//cdn.example.com"))
        assertEquals(false, isValidXUrl(""))
        assertEquals(false, isValidXUrl("   "))
    }
}


