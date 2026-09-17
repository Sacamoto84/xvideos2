package com.client.xvideos.l.ui.screens.explorer.tab.albumSearch

import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import org.junit.Assert.assertEquals
import org.junit.Test

class L_ScreenAlbumSearchTest {

    @Test
    fun `Manga section maps to AlbumType Manga and trims query`() {
        val section = Landing_page_albumSection(title = "Manga")
        val filter = createAlbumSearchFilter(section, "  naruto  ")

        assertEquals(AlbumType.Manga, filter.album_type)
        assertEquals("naruto", filter.searchQuery)
        assertEquals("search_score", filter.display)
        assertEquals(ContentId.All, filter.content_id)
    }

    @Test
    fun `Picture Sets section maps to AlbumType Pictures`() {
        val section = Landing_page_albumSection(title = "Picture Sets")
        val filter = createAlbumSearchFilter(section, "cosplay")

        assertEquals(AlbumType.Pictures, filter.album_type)
        assertEquals("cosplay", filter.searchQuery)
        assertEquals("search_score", filter.display)
        assertEquals(ContentId.All, filter.content_id)
    }

    @Test
    fun `Unknown section title defaults to AlbumType Pictures`() {
        val section = Landing_page_albumSection(title = "Custom Category")
        val filter = createAlbumSearchFilter(section, "art")

        assertEquals(AlbumType.Pictures, filter.album_type)
        assertEquals("art", filter.searchQuery)
    }

    @Test
    fun `Empty query is preserved after trimming`() {
        val section = Landing_page_albumSection(title = "Manga")
        val filter = createAlbumSearchFilter(section, "   ")

        assertEquals("", filter.searchQuery)
    }
}
