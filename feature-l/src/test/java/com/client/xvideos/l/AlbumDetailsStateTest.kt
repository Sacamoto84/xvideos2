package com.client.xvideos.l

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Content
import com.client.xvideos.l.model.Cover
import com.client.xvideos.l.repository.LusciousEndpoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDetailsStateTest {

    @Test
    fun `empty or blank album id is recognized as invalid for saving`() {
        val emptyAlbum = AlbumDetails(
            id = "",
            title = "",
            tags = emptyList(),
            is_manga = false,
            content = Content("", "", ""),
            genres = emptyList(),
            cover = Cover(0, 0, "", ""),
            description = "",
            audiences = emptyList(),
            number_of_pictures = 0,
            number_of_animated_pictures = 0,
            url = "",
            download_url = "",
            created = 0.0,
            modified = 0.0
        )

        assertFalse(emptyAlbum.id.isNotBlank())

        val validAlbum = emptyAlbum.copy(id = "12345", title = "Valid Title")
        assertTrue(validAlbum.id.isNotBlank())
    }

    @Test
    fun `null album details safely yields empty thumbnail and root download URL`() {
        val nullAlbum: AlbumDetails? = null
        val thumbnail = nullAlbum?.cover?.url.orEmpty()
        val downloadUrl = LusciousEndpoints.HOME + nullAlbum?.download_url.orEmpty()

        assertEquals("", thumbnail)
        assertEquals(LusciousEndpoints.HOME, downloadUrl)

        val populatedAlbum = AlbumDetails(
            id = "999",
            title = "Test",
            tags = emptyList(),
            is_manga = false,
            content = Content("", "", ""),
            genres = emptyList(),
            cover = Cover(width = 100, height = 100, size = "normal", url = "https://cdn.example.com/cover.jpg"),
            description = "",
            audiences = emptyList(),
            number_of_pictures = 5,
            number_of_animated_pictures = 2,
            url = "/album/999",
            download_url = "/download/999",
            created = 1000.0,
            modified = 1000.0
        )

        assertEquals("https://cdn.example.com/cover.jpg", populatedAlbum.cover?.url.orEmpty())
        assertEquals("${LusciousEndpoints.HOME}/download/999", LusciousEndpoints.HOME + populatedAlbum.download_url)
    }
}
