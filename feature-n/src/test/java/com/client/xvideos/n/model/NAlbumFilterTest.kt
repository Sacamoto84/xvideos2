package com.client.xvideos.n.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NAlbumFilterTest {

    @Test
    fun `default filter has default flags`() {
        val filter = NAlbumFilter.DEFAULT

        assertTrue(filter.isDefault)
        assertFalse(filter.isFiltered)
        assertEquals("", filter.buildQuery())
        assertEquals(NSortType.RECENT, filter.sort)
    }

    @Test
    fun `filter with language generates correct search query`() {
        val filter = NAlbumFilter(language = "russian")

        assertFalse(filter.isDefault)
        assertTrue(filter.isFiltered)
        assertEquals("language:russian", filter.buildQuery())
    }

    @Test
    fun `filter with non-recent sort is marked as filtered`() {
        val filter = NAlbumFilter(sort = NSortType.POPULAR_ALL_TIME)

        assertFalse(filter.isDefault)
        assertTrue(filter.isFiltered)
        assertEquals("", filter.buildQuery())
    }

    @Test
    fun `search combines manual text with structured filters`() {
        val filter = NAlbumFilter(
            language = "russian",
            sort = NSortType.POPULAR_MONTH,
            tags = listOf(
                NTagCondition(NTag(id = 1, type = "artist", name = "sample artist"), excluded = false),
                NTagCondition(NTag(id = 2, type = "tag", name = "sample tag"), excluded = true)
            ),
            minPages = 20,
            maxPages = 60,
            minFavorites = 100,
            uploadedWithin = NUploadPeriod.WEEK
        )

        val query = filter.buildQuery("  \"sample title\" language:english  ")

        assertEquals(
            "\"sample title\" language:english artist:\"sample artist\" -tag:\"sample tag\" " +
                "pages:>=20 pages:<=60 favorites:>=100 uploaded:<7d",
            query
        )
    }

    @Test
    fun `an excluded tag can be the entire search query`() {
        val filter = NAlbumFilter(tags = listOf(NTagCondition(NTag(id = 1, type = "tag", name = "sample"), true)))

        assertTrue(filter.isFiltered)
        assertEquals("-tag:\"sample\"", filter.buildQuery())
    }
}
