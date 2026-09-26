package com.client.xvideos.r.model

import com.client.xvideos.r.model.tag.TagInfo
import com.client.xvideos.r.model.tag.TagSuggestion
import com.client.xvideos.r.model.tag.TagsResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch57RResponsesTest {

    @Test
    fun `MediaResponse pagination and counts inspection`() {
        val empty = MediaResponse.EMPTY
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)
        assertEquals(0, empty.gifsCount)
        assertEquals(0, empty.usersCount)
        assertEquals(0, empty.nichesCount)
        assertFalse(empty.isLastPage)

        val multiPage = MediaResponse(
            page = 1,
            pages = 3,
            gifs = listOf(GifsInfo(id = "g1"), GifsInfo(id = "g2")),
            users = listOf(UserInfo(username = "u1")),
            niches = listOf(NichesInfo(id = "n1"), NichesInfo(id = "n2"))
        )
        assertFalse(multiPage.isLastPage)
        assertEquals(2, multiPage.gifsCount)
        assertEquals(1, multiPage.usersCount)
        assertEquals(2, multiPage.nichesCount)
        assertTrue(multiPage.isNotEmpty)

        val lastPage = MediaResponse(
            page = 3,
            pages = 3
        )
        assertTrue(lastPage.isLastPage)
    }

    @Test
    fun `CreatorResponse pagination and content inspection`() {
        val empty = CreatorResponse.EMPTY
        assertFalse(empty.isLastPage)
        assertEquals(0, empty.gifsCount)
        assertFalse(empty.hasNiches)

        val response = CreatorResponse(
            page = 1,
            pages = 2,
            gifs = listOf(GifsInfo(id = "g1")),
            niches = listOf(NichesInfo(id = "cat1"))
        )
        assertFalse(response.isLastPage)
        assertEquals(1, response.gifsCount)
        assertTrue(response.hasNiches)
    }

    @Test
    fun `TopCreatorsResponse and TopCreator inspection`() {
        val empty = TopCreatorsResponse.EMPTY
        assertFalse(empty.hasCreators)

        val creator = TopCreator(
            views = 1200,
            description = "Top content creator",
            username = "star"
        )
        assertTrue(creator.hasViews)
        assertTrue(creator.hasDescription)

        val emptyCreator = TopCreator(views = 0, description = "")
        assertFalse(emptyCreator.hasViews)
        assertFalse(emptyCreator.hasDescription)

        val response = TopCreatorsResponse(creators = listOf(creator))
        assertTrue(response.hasCreators)
    }

    @Test
    fun `NichesResponse and Preview inspection`() {
        val empty = NichesResponse.EMPTY
        assertEquals(0, empty.count)
        assertFalse(empty.hasNiches)
        assertTrue(empty.isFirstPage)
        assertFalse(empty.isLastPage)

        val preview = Preview(id = "42", thumbnail = "https://cdn/thumb.jpg")
        assertTrue(preview.hasId)
        assertTrue(preview.hasThumbnail)

        val emptyPreview = Preview(id = "", thumbnail = "")
        assertFalse(emptyPreview.hasId)
        assertFalse(emptyPreview.hasThumbnail)

        val niche = Niche(id = "test", name = "test", previews = listOf(preview))
        val response = NichesResponse(
            page = 1,
            pages = 5,
            niches = listOf(niche)
        )
        assertEquals(1, response.count)
        assertTrue(response.hasNiches)
        assertTrue(response.isFirstPage)
        assertFalse(response.isLastPage)
    }

    @Test
    fun `NichesInfo inspection and displayName fallbacks`() {
        val empty = NichesInfo.EMPTY
        assertEquals("", empty.displayName)
        assertFalse(empty.hasCover)
        assertFalse(empty.hasThumbnail)
        assertFalse(empty.hasDescription)
        assertFalse(empty.hasOwner)
        assertFalse(empty.hasGifs)
        assertFalse(empty.hasSubscribers)

        val full = NichesInfo(
            id = "niche_42",
            name = "Awesome Niche",
            cover = "https://cdn/cover.png",
            thumbnail = "https://cdn/thumb.png",
            description = "Detailed description",
            owner = "manager",
            gifs = 100L,
            subscribers = 500L
        )
        assertEquals("Awesome Niche", full.displayName)
        assertTrue(full.hasCover)
        assertTrue(full.hasThumbnail)
        assertTrue(full.hasDescription)
        assertTrue(full.hasOwner)
        assertTrue(full.hasGifs)
        assertTrue(full.hasSubscribers)

        val idFallback = NichesInfo(id = "niche_only_id", name = "")
        assertEquals("niche_only_id", idFallback.displayName)
    }

    @Test
    fun `TagInfo and TagSuggestion inspection`() {
        val tagWithText = TagInfo(name = "Cosplay")
        assertTrue(tagWithText.hasName)
        val tagBlank = TagInfo(name = "   ")
        assertFalse(tagBlank.hasName)

        val suggestion = TagSuggestion(text = "cosplay")
        assertTrue(suggestion.hasText)
        val emptySuggestion = TagSuggestion(text = "")
        assertFalse(emptySuggestion.hasText)
    }

    @Test
    fun `TagsResponse query helper findByNameOrNull`() {
        val response = TagsResponse(
            tags = listOf(
                TagInfo(name = "Cosplay"),
                TagInfo(name = "Amateur"),
                TagInfo(name = "Animation")
            )
        )
        val found = response.findByNameOrNull("cosplay")
        assertNotNull(found)
        assertEquals("Cosplay", found?.name)

        val uppercaseFound = response.findByNameOrNull("AMATEUR")
        assertNotNull(uppercaseFound)
        assertEquals("Amateur", uppercaseFound?.name)

        assertNull(response.findByNameOrNull("nonexistent"))
        assertNull(response.findByNameOrNull(null))
        assertNull(response.findByNameOrNull(""))
        assertNull(response.findByNameOrNull("   "))
    }
}
