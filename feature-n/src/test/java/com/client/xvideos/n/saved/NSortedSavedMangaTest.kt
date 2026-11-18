package com.client.xvideos.n.saved

import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.model.NSavedMangaOrder
import com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.model.nSortedSavedManga
import org.junit.Assert.assertEquals
import org.junit.Test

class NSortedSavedMangaTest {

    private val item1 = NGalleryPreview(id = 1L, mediaId = 1L, title = "Zebra Manga", numPages = 10)
    private val item2 = NGalleryPreview(id = 2L, mediaId = 2L, title = "Alpha Manga", numPages = 50)
    private val item3 = NGalleryPreview(id = 3L, mediaId = 3L, title = "Beta Manga", numPages = 20)
    private val list = listOf(item1, item2, item3)

    @Test
    fun `nSortedSavedManga sorts by newest reversed`() {
        val sorted = nSortedSavedManga(list, NSavedMangaOrder.NEWEST)
        assertEquals(listOf(item3, item2, item1), sorted)
    }

    @Test
    fun `nSortedSavedManga sorts by oldest`() {
        val sorted = nSortedSavedManga(list, NSavedMangaOrder.OLDEST)
        assertEquals(listOf(item1, item2, item3), sorted)
    }

    @Test
    fun `nSortedSavedManga sorts by title alphabetically`() {
        val sorted = nSortedSavedManga(list, NSavedMangaOrder.TITLE)
        assertEquals(listOf(item2, item3, item1), sorted)
    }

    @Test
    fun `nSortedSavedManga sorts by most pages descending`() {
        val sorted = nSortedSavedManga(list, NSavedMangaOrder.MOST_PAGES)
        assertEquals(listOf(item2, item3, item1), sorted)
    }
}
