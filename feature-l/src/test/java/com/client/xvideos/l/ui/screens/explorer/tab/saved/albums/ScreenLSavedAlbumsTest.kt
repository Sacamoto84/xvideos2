package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums

import com.client.xvideos.l.model.AlbumDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenLSavedAlbumsTest {

    private fun createAlbum(id: String, title: String): AlbumDetails {
        return AlbumDetails(
            id = id,
            title = title,
            cover = null,
            number_of_pictures = 10,
            number_of_animated_pictures = 2
        )
    }

    @Test
    fun `filter valid albums retains only entries with valid numeric id`() {
        val rawList = listOf(
            createAlbum("12345", "Valid Album 1"),
            createAlbum("", "Empty ID"),
            createAlbum("abc_not_a_number", "Invalid ID"),
            createAlbum("67890", "Valid Album 2"),
            createAlbum("   ", "Blank ID"),
        )

        val filtered = rawList.filter { it.id.toLongOrNull() != null }

        assertEquals(2, filtered.size)
        assertEquals(listOf("12345", "67890"), filtered.map { it.id })
    }

    @Test
    fun `filter valid albums handles empty input gracefully`() {
        val emptyList = emptyList<AlbumDetails>()
        val filtered = emptyList.filter { it.id.toLongOrNull() != null }
        assertEquals(0, filtered.size)
    }
}
