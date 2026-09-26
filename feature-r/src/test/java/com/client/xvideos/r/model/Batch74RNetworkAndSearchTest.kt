package com.client.xvideos.r.model

import com.client.xvideos.r.common.search.SuggestionItem
import com.client.xvideos.r.model.search.SearchCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemNichesResponse
import com.client.xvideos.r.model.search.SearchItemTagsResponse
import com.client.xvideos.r.model.search.SearchNichesShortResponse
import com.client.xvideos.r.model.tag.TagInfo
import com.client.xvideos.r.model.tag.TagSuggestion
import com.client.xvideos.r.model.tag.TagsResponse
import com.client.xvideos.r.network.http.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch74RNetworkAndSearchTest {

    @Test
    fun `TagSuggestion formatCount and isSameSuggestion`() {
        val suggestion1 = TagSuggestion(text = "anime", gifs = 1_500_000L, type = "tag")
        val suggestion2 = TagSuggestion(text = " Anime ", gifs = 500L, type = "tag")
        val suggestionNiche = TagSuggestion(text = "anime", gifs = 2_400L, type = "niche")

        assertEquals("1.5M", suggestion1.formatCount())
        assertEquals("500", suggestion2.formatCount())
        assertEquals("2.4k", suggestionNiche.formatCount())

        assertTrue(suggestion1.isSameSuggestion(suggestion2))
        assertFalse(suggestion1.isSameSuggestion(suggestionNiche))
        assertFalse(suggestion1.isSameSuggestion(null))

        val fromTag = TagSuggestion.fromTagInfo(TagInfo(name = "gaming", count = 100L))
        assertEquals("gaming", fromTag.text)
        assertEquals(100L, fromTag.gifs)
        assertEquals("tag", fromTag.type)
    }

    @Test
    fun `TagInfo and TagsResponse helpers and sorting`() {
        val tag1 = TagInfo.fromName(" Cosplay ", count = 12_500L)
        assertEquals("Cosplay", tag1.name)
        assertEquals("12.5k", tag1.formatCount())

        val tag2 = TagInfo(name = "dance", count = 5_000_000L)
        assertEquals("5.0M", tag2.formatCount())

        val response = TagsResponse(tags = listOf(tag1, tag2))
        assertEquals(listOf("Cosplay", "dance"), response.allNames)

        val filtered = response.filterByQuery("play")
        assertEquals(1, filtered.size)
        assertEquals("Cosplay", filtered.first().name)

        val sorted = response.sortedByCountDescending()
        assertEquals("dance", sorted.tags.first().name)
        assertEquals("Cosplay", sorted.tags.last().name)
    }

    @Test
    fun `SearchCreatorsResponse filters and conversions`() {
        val creator1 = SearchItemCreatorsResponse(
            text = "@Alice",
            name = "Alice Wonderland",
            verified = true,
            followers = 1_200_000L
        )
        val creator2 = SearchItemCreatorsResponse(
            text = "@Bob",
            name = "Bob Builder",
            verified = false,
            followers = 3_400L
        )
        val response = SearchCreatorsResponse(items = listOf(creator1, creator2))

        assertEquals(listOf("Alice", "Bob"), response.allUsernames)
        assertEquals(1, response.verifiedOnly.size)
        assertEquals("Alice", response.verifiedOnly.first().username)

        val filtered = response.filterByQuery("builder")
        assertEquals(1, filtered.size)
        assertEquals("Bob", filtered.first().username)

        assertEquals("alice", creator1.normalizedUsername)
        assertEquals("1.2M", creator1.formatFollowers())
        assertEquals("3.4k", creator2.formatFollowers())
        assertTrue(creator1.matches("alice"))
        assertTrue(creator1.matches("wonder"))
        assertFalse(creator1.matches("xyz"))

        val userInfo = creator1.toUserInfo()
        assertEquals("Alice", userInfo.username)
        assertEquals("Alice Wonderland", userInfo.name)
        assertTrue(userInfo.verified)
        assertEquals(1_200_000L, userInfo.followers)
    }

    @Test
    fun `SearchItemNichesResponse formatters and matching`() {
        val niche1 = SearchItemNichesResponse(
            id = "soft-cosplay",
            name = "Soft Cosplay",
            gifs = 250_000L,
            subscribers = 1_500_000L,
            tags = listOf("cosplay", "costume")
        )
        val niche2 = SearchItemNichesResponse(
            id = "gaming-highlights",
            name = "Gaming",
            gifs = 800L,
            subscribers = 400L
        )
        val response = SearchNichesShortResponse(niches = listOf(niche1, niche2))

        val found = response.findByIdOrNull("soft-cosplay")
        assertNotNull(found)
        assertEquals("Soft Cosplay", found?.name)
        assertNull(response.findByIdOrNull("unknown"))

        val filtered = response.filterByQuery("costume")
        assertEquals(1, filtered.size)
        assertEquals("soft-cosplay", filtered.first().id)

        assertEquals("soft-cosplay", niche1.normalizedId)
        assertTrue(niche1.matches("costume"))
        assertTrue(niche1.matches("cosplay"))
        assertFalse(niche1.matches("unknown"))

        assertEquals("250.0k", niche1.formatGifs())
        assertEquals("1.5M", niche1.formatSubscribers())
        assertEquals("800", niche2.formatGifs())
        assertEquals("400", niche2.formatSubscribers())
    }

    @Test
    fun `SearchItemTagsResponse formatters and converters`() {
        val tagItem = SearchItemTagsResponse(text = " cyberpunk ", gifs = 45_000L)
        assertEquals("cyberpunk", tagItem.normalizedText)
        assertTrue(tagItem.matches("punk"))
        assertFalse(tagItem.matches("steampunk"))
        assertEquals("45.0k", tagItem.formatGifs())

        val tagInfo = tagItem.toTagInfo()
        assertEquals("cyberpunk", tagInfo.name)
        assertEquals(45_000L, tagInfo.count)

        val fromTag = SearchItemTagsResponse.fromTagInfo(TagInfo(name = "retro", count = 99L))
        assertEquals("retro", fromTag.text)
        assertEquals(99L, fromTag.gifs)
    }

    @Test
    fun `SuggestionItem bridges and formatting`() {
        val suggestion = SuggestionItem(text = " cyberpunk ", count = 120_000L)
        assertEquals("120.0k", suggestion.formatCount())
        assertTrue(suggestion.matchesQuery("cyber"))
        assertTrue(suggestion.matchesQuery(null))

        val tagInfo = suggestion.toTagInfo()
        assertEquals("cyberpunk", tagInfo.name)
        assertEquals(120_000L, tagInfo.count)

        val tagSuggestion = suggestion.toTagSuggestion("niche")
        assertEquals("cyberpunk", tagSuggestion.text)
        assertEquals(120_000L, tagSuggestion.gifs)
        assertEquals("niche", tagSuggestion.type)

        val fromTag = SuggestionItem.fromTagInfo(TagInfo(name = "synthwave", count = 77L))
        assertEquals("synthwave", fromTag.text)
        assertEquals(77L, fromTag.count)

        val fromSug = SuggestionItem.fromTagSuggestion(TagSuggestion(text = "lofi", gifs = 88L))
        assertEquals("lofi", fromSug.text)
        assertEquals(88L, fromSug.count)
    }

    @Test
    fun `Route API versions and factories`() {
        val getRoute = Route.get("/v2/gifs/search?query={q}&page={page}", "q" to "test", "page" to 1)
        assertTrue(getRoute.isGet)
        assertFalse(getRoute.isPost)
        assertTrue(getRoute.isApiV2)
        assertFalse(getRoute.isApiV1)
        assertEquals("/v2/gifs/search", getRoute.pathWithoutQuery)
        assertTrue(getRoute.matchesPath("/v2/gifs/search"))

        val postRoute = Route.post("/v1/auth/login")
        assertTrue(postRoute.isPost)
        assertTrue(postRoute.isApiV1)
        assertFalse(postRoute.isApiV2)
        assertTrue(postRoute.matchesPath("/v1/auth/login"))
    }
}
