package com.client.xvideos.l.net

import com.client.xvideos.l.model.PicsDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тестирование вычисления пагинации и нормализации медиа-ссылок альбома Luscious.
 */
class AlbumPicsDetailsUtilsTest {

    @Test
    fun `calculateAlbumPages handles nulls and zeroes gracefully`() {
        assertEquals(1, calculateAlbumPages(null, null, null))
        assertEquals(1, calculateAlbumPages(0, 0, 0))
        assertEquals(1, calculateAlbumPages(-5, -10, 0))
    }

    @Test
    fun `calculateAlbumPages computes correct ceil division`() {
        assertEquals(1, calculateAlbumPages(null, 50, 50))
        assertEquals(2, calculateAlbumPages(null, 51, 50))
        assertEquals(3, calculateAlbumPages(null, 101, 50))
        assertEquals(10, calculateAlbumPages(null, 1000, 100))
    }

    @Test
    fun `calculateAlbumPages takes maximum of server pages and calculated pages`() {
        // Сервер говорит 5 страниц, хотя по элементам вышло 3 -> берём 5
        assertEquals(5, calculateAlbumPages(5, 101, 50))
        // Сервер говорит 2 страницы, но элементов 101 при 50 на страницу -> берём 3
        assertEquals(3, calculateAlbumPages(2, 101, 50))
    }

    @Test
    fun `normalizePictureUrls detects animated gif and video files`() {
        val items = listOf(
            PicsDetails(
                height = 200,
                width = 200,
                is_animated = false,
                url_to_original = "https://example.com/animation.gif?size=orig"
            ),
            PicsDetails(
                height = 200,
                width = 200,
                is_animated = false,
                url_to_original = "https://example.com/clip.mp4"
            ),
            PicsDetails(
                height = 200,
                width = 200,
                is_animated = false,
                url_to_original = "https://example.com/photo.jpg"
            )
        )

        val normalized = normalizePictureUrls(items)

        assertTrue(normalized[0].is_animated)
        assertTrue(normalized[1].is_animated)
        assertFalse(normalized[2].is_animated)
    }
}
