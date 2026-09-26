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
}
