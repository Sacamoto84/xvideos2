package com.client.xvideos.l.model

import org.junit.Assert.assertEquals
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
}
