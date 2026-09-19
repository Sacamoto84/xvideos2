package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.createAlbumSearchFilter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ScreenCollectionNameSerializationTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos -> oos.writeObject(value) }
            baos.toByteArray()
        }
        return ByteArrayInputStream(bytes).use { bais ->
            ObjectInputStream(bais).use { ois -> ois.readObject() as T }
        }
    }

    @Test
    fun `ScreenCollectionName in feature-l serializes and deserializes properly`() {
        val screen = ScreenCollectionName(collectionName = "MangaCollection", popOnBack = true)
        val restored = roundTrip(screen)
        assertEquals("MangaCollection", restored.collectionName)
        assertEquals("LCollection:MangaCollection:true", restored.key)
    }

    @Test
    fun `createAlbumSearchFilter handles Manga and Picture Sets correctly`() {
        val mangaSection = Landing_page_albumSection(title = "Manga", items = emptyList())
        val mangaFilter = createAlbumSearchFilter(mangaSection, "cosplay")
        assertEquals(AlbumType.Manga, mangaFilter.album_type)
        assertEquals("cosplay", mangaFilter.searchQuery)

        val picturesSection = Landing_page_albumSection(title = "Picture Sets", items = emptyList())
        val picturesFilter = createAlbumSearchFilter(picturesSection, "art")
        assertEquals(AlbumType.Pictures, picturesFilter.album_type)
        assertEquals("art", picturesFilter.searchQuery)
    }

    @Test
    fun `hasNoSearchResults detects null and empty sections correctly`() {
        org.junit.Assert.assertFalse(
            com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.hasNoSearchResults(null)
        )

        val emptyType = com.client.xvideos.l.model.Landing_page_albumType(
            sections = emptyList(),
            title = "Search"
        )
        org.junit.Assert.assertTrue(
            com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.hasNoSearchResults(emptyType)
        )

        val emptySectionsType = com.client.xvideos.l.model.Landing_page_albumType(
            sections = listOf(Landing_page_albumSection(title = "Manga", items = emptyList())),
            title = "Search"
        )
        org.junit.Assert.assertTrue(
            com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.hasNoSearchResults(emptySectionsType)
        )
    }
}
