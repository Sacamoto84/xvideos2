package com.client.xvideos.r.model

import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.r.model.search.SearchCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemNichesResponse
import com.client.xvideos.r.model.search.SearchItemTagsResponse
import com.client.xvideos.r.model.search.SearchNichesShortResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch58RSearchAndSavedTest {

    @Test
    fun `SearchCreatorsResponse inspection and findByUsernameOrNull`() {
        val empty = SearchCreatorsResponse.EMPTY
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)
        assertEquals(0, empty.count)
        assertFalse(empty.hasItems)
        assertNull(empty.firstOrNull)
        assertNull(empty.findByUsernameOrNull("anyone"))

        val creator1 = SearchItemCreatorsResponse(
            text = "@alex_model",
            name = "Alex Model",
            image = "https://cdn/avatar.jpg",
            verified = true,
            followers = 1500L
        )
        val creator2 = SearchItemCreatorsResponse(
            text = "@sam_star",
            name = "",
            image = null,
            followers = 0L
        )

        val response = SearchCreatorsResponse(items = listOf(creator1, creator2))
        assertFalse(response.isEmpty)
        assertTrue(response.isNotEmpty)
        assertEquals(2, response.count)
        assertTrue(response.hasItems)
        assertEquals(creator1, response.firstOrNull)

        // Lookup with and without '@', case-insensitive
        val foundWithAt = response.findByUsernameOrNull("@alex_model")
        assertNotNull(foundWithAt)
        assertEquals("Alex Model", foundWithAt?.displayName)

        val foundWithoutAt = response.findByUsernameOrNull("ALEX_MODEL")
        assertNotNull(foundWithoutAt)
        assertEquals("Alex Model", foundWithoutAt?.displayName)

        val foundFallback = response.findByUsernameOrNull("sam_star")
        assertNotNull(foundFallback)
        assertEquals("sam_star", foundFallback?.displayName)

        assertNull(response.findByUsernameOrNull("unknown"))
        assertNull(response.findByUsernameOrNull(null))
        assertNull(response.findByUsernameOrNull(""))
        assertNull(response.findByUsernameOrNull("   "))
    }

    @Test
    fun `SearchItemCreatorsResponse state flags and predicates`() {
        val valid = SearchItemCreatorsResponse(
            text = "@creator_x",
            name = "Creator X",
            image = "https://cdn/img.png",
            followers = 200L
        )
        assertTrue(valid.isValid)
        assertTrue(valid.hasText)
        assertTrue(valid.hasName)
        assertTrue(valid.hasImage)
        assertTrue(valid.hasFollowers)
        assertEquals("creator_x", valid.username)
        assertEquals("Creator X", valid.displayName)

        val empty = SearchItemCreatorsResponse.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasText)
        assertFalse(empty.hasName)
        assertFalse(empty.hasImage)
        assertFalse(empty.hasFollowers)
        assertEquals("", empty.username)
        assertEquals("", empty.displayName)
    }

    @Test
    fun `SearchNichesShortResponse pagination and collection inspection`() {
        val empty = SearchNichesShortResponse.EMPTY
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)
        assertEquals(0, empty.count)
        assertFalse(empty.hasNiches)
        assertTrue(empty.isFirstPage)
        assertFalse(empty.isLastPage)
        assertNull(empty.firstOrNull)

        val niche = SearchItemNichesResponse(
            id = "fitness",
            name = "Fitness",
            gifs = 450L,
            subscribers = 1200L,
            tags = listOf("fit", "workout"),
            preferences = listOf("straight"),
            thumbnail = "https://cdn/thumb.jpg"
        )

        val pagedResponse = SearchNichesShortResponse(
            page = 1L,
            pages = 3L,
            total = 15L,
            niches = listOf(niche)
        )
        assertFalse(pagedResponse.isEmpty)
        assertTrue(pagedResponse.isNotEmpty)
        assertEquals(1, pagedResponse.count)
        assertTrue(pagedResponse.hasNiches)
        assertTrue(pagedResponse.isFirstPage)
        assertFalse(pagedResponse.isLastPage)
        assertEquals(niche, pagedResponse.firstOrNull)

        val lastPageResponse = SearchNichesShortResponse(
            page = 3L,
            pages = 3L,
            total = 15L,
            niches = listOf(niche)
        )
        assertTrue(lastPageResponse.isLastPage)
    }

    @Test
    fun `SearchItemNichesResponse properties and fallbacks`() {
        val niche = SearchItemNichesResponse(
            id = "dance_moves",
            name = "Dance Moves",
            gifs = 100L,
            subscribers = 50L,
            tags = listOf("dance"),
            preferences = listOf("all"),
            thumbnail = "https://cdn/dance.png"
        )
        assertTrue(niche.isValid)
        assertTrue(niche.hasName)
        assertTrue(niche.hasThumbnail)
        assertTrue(niche.hasGifs)
        assertTrue(niche.hasSubscribers)
        assertTrue(niche.hasTags)
        assertTrue(niche.hasPreferences)
        assertEquals("Dance Moves", niche.displayName)

        val fallbackNiche = SearchItemNichesResponse(
            id = "only_id",
            name = ""
        )
        assertEquals("only_id", fallbackNiche.displayName)
        assertFalse(fallbackNiche.hasName)
        assertFalse(fallbackNiche.hasThumbnail)
        assertFalse(fallbackNiche.hasPreferences)
    }

    @Test
    fun `SearchItemTagsResponse state inspection`() {
        val empty = SearchItemTagsResponse.EMPTY
        assertFalse(empty.isValid)
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)
        assertFalse(empty.hasText)
        assertFalse(empty.hasGifs)

        val populated = SearchItemTagsResponse(
            text = "outdoor",
            gifs = 240L
        )
        assertTrue(populated.isValid)
        assertFalse(populated.isEmpty)
        assertTrue(populated.isNotEmpty)
        assertTrue(populated.hasText)
        assertTrue(populated.hasGifs)
    }

    @Test
    fun `CollectionEntity inspection helpers`() {
        val emptyEntity = CollectionEntity<GifsInfo>(collection = "", items = emptyList())
        assertTrue(emptyEntity.isEmpty)
        assertFalse(emptyEntity.isNotEmpty)
        assertEquals(0, emptyEntity.size)
        assertFalse(emptyEntity.isValid)
        assertNull(emptyEntity.firstOrNull())
        assertNull(emptyEntity.getOrNull(0))

        val item1 = GifsInfo(id = "gif_1")
        val item2 = GifsInfo(id = "gif_2")
        val entity = CollectionEntity(collection = "Favorites 2026", items = listOf(item1, item2))
        assertFalse(entity.isEmpty)
        assertTrue(entity.isNotEmpty)
        assertEquals(2, entity.size)
        assertTrue(entity.isValid)
        assertEquals(item1, entity.firstOrNull())
        assertEquals(item2, entity.getOrNull(1))
        assertNull(entity.getOrNull(5))
    }
}
