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
        assertEquals("", filter.searchQuery)
        assertEquals(NSortType.RECENT, filter.sort)
    }

    @Test
    fun `filter with language generates correct search query`() {
        val filter = NAlbumFilter(language = "russian")

        assertFalse(filter.isDefault)
        assertTrue(filter.isFiltered)
        assertEquals("language:russian", filter.searchQuery)
    }

    @Test
    fun `filter with non-recent sort is marked as filtered`() {
        val filter = NAlbumFilter(sort = NSortType.POPULAR_ALL_TIME)

        assertFalse(filter.isDefault)
        assertTrue(filter.isFiltered)
        assertEquals("", filter.searchQuery)
    }
}
