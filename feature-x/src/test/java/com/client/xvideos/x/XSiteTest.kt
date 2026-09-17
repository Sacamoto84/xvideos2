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
}


