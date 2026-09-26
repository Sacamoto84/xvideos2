package com.client.xvideos.l.model

import com.client.xvideos.l.repository.RepositoryResult
import com.client.xvideos.l.repository.errorMessageOrNull
import com.client.xvideos.l.repository.getOrNull
import com.client.xvideos.l.repository.isError
import com.client.xvideos.l.repository.isLoading
import com.client.xvideos.l.repository.isSuccess
import com.client.xvideos.l.repository.throwableOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumFilterDisplayStructureTest {

    @Test
    fun `all filter display items have non-blank fields`() {
        assertTrue(albumFilterDisplay.isNotEmpty())
        for (item in albumFilterDisplay) {
            assertTrue("Primary must not be blank for $item", item.primary.isNotBlank())
            assertTrue("Secondary must not be blank for $item", item.secondary.isNotBlank())
            assertTrue("Request must not be blank for $item", item.request.isNotBlank())
        }
    }

    @Test
    fun `all request keys are unique across albumFilterDisplay`() {
        val requestKeys = albumFilterDisplay.map { it.request }
        val uniqueKeys = requestKeys.toSet()
        assertEquals("Each filter option must map to a unique request query", requestKeys.size, uniqueKeys.size)
    }

    @Test
    fun `items belong exclusively to predefined category constants`() {
        val validCategories = setOf(byDate, byTopRated, byFirstLetter)
        val categories = albumFilterDisplay.map { it.primary }.toSet()
        assertEquals(validCategories, categories)
    }

    @Test
    fun `first letter filter covers all latin letters plus Any`() {
        val letterItems = albumFilterDisplay.filter { it.primary == byFirstLetter }
        assertEquals(27, letterItems.size)
        assertEquals("Any", letterItems.first().secondary)
        assertEquals("alpha_any", letterItems.first().request)

        val expectedLetters = ('A'..'Z').map { it.toString() }
        val actualLetters = letterItems.drop(1).map { it.secondary }
        assertEquals(expectedLetters, actualLetters)
    }

    @Test
    fun `top rated filter options contain expected ranges`() {
        val topRatedItems = albumFilterDisplay.filter { it.primary == byTopRated }
        val secondaries = topRatedItems.map { it.secondary }
        assertTrue(secondaries.contains("7 Days"))
        assertTrue(secondaries.contains("14 Days"))
        assertTrue(secondaries.contains("30 Days"))
        assertTrue(secondaries.contains("90 Days"))
        assertTrue(secondaries.contains("1 Year"))
        assertTrue(secondaries.contains("All Time"))
    }

    @Test
    fun `DataAlbumFilterDisplay and lookup operate correctly`() {
        val empty = DataAlbumFilterDisplay.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.isByTopRated)

        val item = findAlbumFilterDisplayByRequest("rating_7_days")
        assertNotNull(item)
        assertTrue(item!!.isValid)
        assertTrue(item.isByTopRated)
        assertFalse(item.isByDate)
        assertFalse(item.isByFirstLetter)

        assertNull(findAlbumFilterDisplayByRequest("non_existent"))
        assertNull(findAlbumFilterDisplayByRequest(""))
    }

    @Test
    fun `RepositoryResult extensions operate correctly`() {
        val loading: RepositoryResult = RepositoryResult.Loading
        assertTrue(loading.isLoading)
        assertFalse(loading.isSuccess)

        val success: RepositoryResult = RepositoryResult.Success("test_data")
        assertTrue(success.isSuccess)
        assertEquals("test_data", success.getOrNull<String>())

        val error: RepositoryResult = RepositoryResult.Error("Network error")
        assertTrue(error.isError)
        assertEquals("Network error", error.errorMessageOrNull())
        assertNull(error.throwableOrNull())
    }

    @Test
    fun `PicsDetailsMedia and image url helpers work accurately`() {
        assertEquals(true, "https://cdn/test.jpg".isLImageFileUrl())
        assertEquals(true, "https://cdn/test.png".isLImageFileUrl())
        assertEquals(false, "https://cdn/test.mp4".isLImageFileUrl())

        val picWithoutId = PicsDetails(id = null, url = "https://example.com/id/98765/pic")
        assertEquals(true, picWithoutId.hasAnchorId)
        assertEquals("98765", picWithoutId.extractAnchorIdOrEmpty())

        val emptyPic = PicsDetails.EMPTY
        assertEquals(false, emptyPic.hasAnchorId)
        assertEquals("", emptyPic.extractAnchorIdOrEmpty())
    }

    @Test
    fun `FilterGenre and OnlyContent properties work accurately`() {
        val genre = FilterGenre(id = "1", title = "Yaoi", posterUrl = "https://cdn/poster.jpg", description = "Desc")
        assertTrue(genre.isValid)
        assertTrue(genre.hasPoster)
        assertTrue(genre.hasDescription)
        assertFalse(genre.hasParent)

        val onlyContent = OnlyContent(id = "c1", title = "Hentai", url = "/hentai")
        assertTrue(onlyContent.isValid)
        assertTrue(onlyContent.hasUrl)
    }

    @Test
    fun `SavedAlbumFilter and AlbumListFilter properties reflect filter parameters`() {
        val emptySaved = SavedAlbumFilter.EMPTY
        assertTrue(emptySaved.isEmpty)
        assertFalse(emptySaved.isNotEmpty)
        assertFalse(emptySaved.hasFilter)

        val activeFilter = AlbumListFilter(
            searchQuery = "gothic",
            selection = "animated",
            tagPlus = listOf("t1", "t2")
        )
        assertTrue(activeFilter.hasSearchQuery)
        assertTrue(activeFilter.hasSelection)
        assertTrue(activeFilter.isAnimatedOnly)
        assertTrue(activeFilter.isFiltered)
        assertEquals(4, activeFilter.totalFilterCount) // 1 query + 1 selection + 2 tags

        val savedWithFilter = SavedAlbumFilter(name = "Saved Gothic", filter = activeFilter)
        assertFalse(savedWithFilter.isEmpty)
        assertTrue(savedWithFilter.isNotEmpty)
        assertTrue(savedWithFilter.hasFilter)
    }
}
