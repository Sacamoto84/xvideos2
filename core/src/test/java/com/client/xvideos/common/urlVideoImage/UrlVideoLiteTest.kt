package com.client.xvideos.common.urlVideoImage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlVideoLiteTest {

    @Test
    fun `normalizedPreviewUrlOrNull отсекает пустые, пробельные и строковые null`() {
        assertNull("".normalizedPreviewUrlOrNull())
        assertNull("   ".normalizedPreviewUrlOrNull())
        assertNull("\t\n".normalizedPreviewUrlOrNull())
        assertNull("null".normalizedPreviewUrlOrNull())
        assertNull("NULL".normalizedPreviewUrlOrNull())
        assertNull("  null  ".normalizedPreviewUrlOrNull())

        assertEquals("https://example.com/video.mp4", "  https://example.com/video.mp4  ".normalizedPreviewUrlOrNull())
    }

    @Test
    fun `withHost заменяет хост для URL со схемой и путем`() {
        val url = "https://original-cdn.com/videos/hls/test.mp4?token=123"
        val replaced = url.withHost("thumb-cdn77.xvideos-cdn.com")
        assertEquals("https://thumb-cdn77.xvideos-cdn.com/videos/hls/test.mp4?token=123", replaced)
    }

    @Test
    fun `withHost корректно обрабатывает URL без слэша в пути`() {
        val url = "http://cdn.com"
        val replaced = url.withHost("newhost.com")
        assertEquals("http://newhost.com", replaced)
    }

    @Test
    fun `withHost возвращает null для строк без схемы`() {
        assertNull("just-a-string".withHost("example.com"))
        assertNull("/relative/path.mp4".withHost("example.com"))
    }

    @Test
    fun `xPreviewVideoCandidates генерирует уникальный список с CDN-зеркалами и фильтрует некорректные`() {
        val primary = "https://cdn.example.com/videos/thumb.mp4"
        val fallbacks = listOf("  ", "null", "https://cdn.example.com/videos/thumb.mp4", "https://backup.org/video.mp4")

        val candidates = xPreviewVideoCandidates(primary, fallbacks)

        // Первый кандидат - исходный основной URL
        assertEquals(primary, candidates.first())

        // Содержит CDN-зеркала для primary
        assertTrue(candidates.contains("https://thumb-cdn77.xvideos-cdn.com/videos/thumb.mp4"))
        assertTrue(candidates.contains("https://thumbs-gcore.xvideos-cdn.com/videos/thumb.mp4"))
        assertTrue(candidates.contains("https://cdn77-pic.xvideos-cdn.com/videos/thumb.mp4"))
        assertTrue(candidates.contains("https://gcore-pic.xvideos-cdn.com/videos/thumb.mp4"))

        // Содержит резервный URL
        assertTrue(candidates.contains("https://backup.org/video.mp4"))

        // Все кандидаты уникальны
        assertEquals(candidates.size, candidates.distinct().size)
    }
}
