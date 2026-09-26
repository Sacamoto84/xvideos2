package com.client.xvideos.r.model

import androidx.compose.ui.text.input.TextFieldValue
import com.client.xvideos.r.common.search.IDaoSearchTemplate
import com.client.xvideos.r.common.search.ISearchTemplate
import com.client.xvideos.r.common.search.SuggestionItem
import com.client.xvideos.r.model.tag.TagInfo
import com.client.xvideos.r.model.tag.TagSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch64RModelsAndSearchTest {

    @Test
    fun `Order and MediaType flags and resolvers`() {
        assertTrue(Order.TOP_WEEK.isTopWeek)
        assertFalse(Order.TOP.isTopWeek)

        assertTrue(Order.TOP_MONTH.isTopMonth)
        assertTrue(Order.TOP28.isTopMonth)
        assertFalse(Order.TOP_WEEK.isTopMonth)

        assertTrue(Order.FORCE_TEMP.isForceTemp)
        assertFalse(Order.LATEST.isForceTemp)

        assertEquals(Order.TOP_WEEK, Order.fromNameOrNull("TOP_WEEK"))
        assertEquals(Order.LATEST, Order.fromNameOrNull("latest"))
        assertNull(Order.fromNameOrNull(null))
        assertNull(Order.fromNameOrNull("nonexistent"))

        assertEquals(Order.TOP_WEEK, Order.fromNameOrDefault("TOP_WEEK"))
        assertEquals(Order.LATEST, Order.fromNameOrDefault("unknown"))

        assertEquals(Order.TOP_WEEK, Order.fromStringOrNull("top7"))
        assertEquals(Order.TOP_WEEK, Order.fromStringOrNull("TOP_WEEK"))
        assertNull(Order.fromStringOrNull(null))
        assertNull(Order.fromStringOrNull("invalid"))

        // MediaType
        assertEquals(MediaType.GIF, MediaType.fromNameOrNull("GIF"))
        assertEquals(MediaType.IMAGE, MediaType.fromNameOrNull("image"))
        assertNull(MediaType.fromNameOrNull(null))
        assertNull(MediaType.fromNameOrNull("unknown"))

        assertEquals(MediaType.GIF, MediaType.fromName("GIF"))
        assertEquals(MediaType.ALL, MediaType.fromName("invalid"))
    }

    @Test
    fun `URL1 video and image presence, best URLs, and containsUrl`() {
        val empty = URL1.EMPTY
        assertFalse(empty.hasAnyVideoUrl)
        assertFalse(empty.hasAnyImageUrl)
        assertFalse(empty.containsUrl("test"))
        assertFalse(empty.containsUrl(null))

        val mediaUrls = URL1(
            thumbnail = "https://cdn/thumb.jpg",
            silent = "https://cdn/silent.mp4",
            poster = "https://cdn/poster.jpg",
            html = "https://redgifs.com/ifr/test",
            sd = "https://cdn/sd.mp4",
            hd = "https://cdn/hd.mp4"
        )

        assertTrue(mediaUrls.hasAnyVideoUrl)
        assertTrue(mediaUrls.hasAnyImageUrl)
        assertEquals("https://cdn/hd.mp4", mediaUrls.bestDownloadUrl)
        assertTrue(mediaUrls.containsUrl("thumb"))
        assertTrue(mediaUrls.containsUrl("SILENT"))
        assertTrue(mediaUrls.containsUrl("poster.jpg"))
        assertTrue(mediaUrls.containsUrl("ifr/test"))
        assertTrue(mediaUrls.containsUrl("hd.mp4"))
        assertFalse(mediaUrls.containsUrl("nonexistent"))

        val silentOnly = URL1(sd = "https://cdn/sd.mp4", silent = "https://cdn/silent.mp4")
        assertEquals("https://cdn/silent.mp4", silentOnly.bestDownloadUrl)

        val sdOnly = URL1(sd = "https://cdn/sd.mp4")
        assertEquals("https://cdn/sd.mp4", sdOnly.bestDownloadUrl)
    }

    @Test
    fun `TagInfo normalization, matching, and equality`() {
        val tag = TagInfo(name = " Cosplay Girl ", count = 125L)
        assertEquals("cosplay girl", tag.normalizedName)
        assertTrue(tag.matches("cosplay"))
        assertTrue(tag.matches("GIRL"))
        assertFalse(tag.matches("anime"))
        assertFalse(tag.matches(null))
        assertFalse(tag.matches(""))

        val sameTag = TagInfo(name = "cosplay girl", count = 50L)
        assertTrue(tag.isSameTag(sameTag))
        assertFalse(tag.isSameTag(null))
        assertFalse(tag.isSameTag(TagInfo(name = "other")))
    }

    @Test
    fun `TagSuggestion normalization, type checks, and toTagInfo`() {
        val suggestion = TagSuggestion(
            text = " Blonde ",
            gifs = 2500L,
            type = "tag"
        )
        assertEquals("blonde", suggestion.normalizedText)
        assertTrue(suggestion.matches("blon"))
        assertTrue(suggestion.matches("BLONDE"))
        assertFalse(suggestion.matches("brunette"))
        assertFalse(suggestion.matches(null))

        assertTrue(suggestion.isTagType)
        assertFalse(suggestion.isNicheType)
        assertFalse(suggestion.isCreatorType)

        val nicheSuggestion = TagSuggestion(text = "milf", type = "niche")
        assertTrue(nicheSuggestion.isNicheType)
        assertFalse(nicheSuggestion.isTagType)

        val creatorSuggestion = TagSuggestion(text = "model_x", type = "creator")
        assertTrue(creatorSuggestion.isCreatorType)

        val userSuggestion = TagSuggestion(text = "model_y", type = "user")
        assertTrue(userSuggestion.isCreatorType)

        val convertedTag = suggestion.toTagInfo()
        assertEquals("Blonde", convertedTag.name)
        assertEquals(2500L, convertedTag.count)
    }

    @Test
    fun `NichesInfo rules, best image, matching, and equality`() {
        val empty = NichesInfo.EMPTY
        assertFalse(empty.hasRules)
        assertEquals("", empty.bestImageUrl)
        assertFalse(empty.matches("test"))

        val niche = NichesInfo(
            id = "cosplay",
            name = "Cosplay Niche",
            thumbnail = "https://cdn/thumb.jpg",
            cover = "https://cdn/cover.jpg",
            rules = "1. Be respectful"
        )
        assertTrue(niche.hasRules)
        assertEquals("https://cdn/cover.jpg", niche.bestImageUrl)
        assertTrue(niche.matches("cosplay"))
        assertTrue(niche.matches("NICHE"))
        assertFalse(niche.matches("other"))
        assertFalse(niche.matches(null))

        val sameNiche = NichesInfo(id = "cosplay", name = "Different Name")
        assertTrue(niche.isSameNiche(sameNiche))
        assertFalse(niche.isSameNiche(null))
        assertFalse(niche.isSameNiche(NichesInfo(id = "other")))

        val thumbOnlyNiche = NichesInfo(id = "n1", thumbnail = "https://cdn/thumb.jpg", cover = null)
        assertEquals("https://cdn/thumb.jpg", thumbOnlyNiche.bestImageUrl)
    }

    @Test
    fun `UserInfo views, normalized username, matching, and equality`() {
        val user = UserInfo(
            username = " Alex_Star ",
            name = "Alexandria",
            views = 1500000L
        )
        assertTrue(user.hasViews)
        assertEquals("alex_star", user.normalizedUsername)
        assertTrue(user.matches("alex"))
        assertTrue(user.matches("STAR"))
        assertTrue(user.matches("Alexandria"))
        assertFalse(user.matches("Unknown"))
        assertFalse(user.matches(null))

        val sameUser = UserInfo(username = "alex_star", name = "Alex")
        assertTrue(user.isSameUser(sameUser))
        assertFalse(user.isSameUser(null))
        assertFalse(user.isSameUser(UserInfo(username = "other_star")))

        val noViewsUser = UserInfo(username = "newbie", views = 0L)
        assertFalse(noViewsUser.hasViews)
    }

    @Test
    fun `SuggestionItem and ISearchTemplate helpers`() = runTest {
        val item = SuggestionItem(text = " Anime ", count = 42L)
        assertTrue(item.isValid)
        assertTrue(item.hasCount)
        assertEquals("anime", item.normalizedText)
        assertTrue(item.matches("ani"))
        assertTrue(item.matches("ANIME"))
        assertFalse(item.matches("cosplay"))
        assertFalse(item.matches(null))

        val emptyItem = SuggestionItem()
        assertFalse(emptyItem.isValid)
        assertFalse(emptyItem.hasCount)

        val fakeDao = object : IDaoSearchTemplate {
            override fun observeAllTexts(): Flow<List<String>> = flowOf(emptyList())
            override suspend fun insertAndTrim(text: String, limit: Int) {}
            override suspend fun deleteByTexts(text: String) {}
            override suspend fun deleteAll() {}
        }

        val testScope = TestScope(testScheduler)
        val template = object : ISearchTemplate(testScope, fakeDao) {}

        assertTrue(template.isSearchTextEmpty)
        assertFalse(template.isSearchActive)
        assertEquals(0, template.suggestionsCount)
        assertEquals(0, template.stackSize)

        template.searchText.value = TextFieldValue("search term")
        assertFalse(template.isSearchTextEmpty)

        template.searchTextDone.value = "search term"
        assertTrue(template.isSearchActive)

        template.searchTextSuggestions.value = listOf(item)
        assertEquals(1, template.suggestionsCount)

        template.pushHistory("query 1")
        template.pushHistory("query 2")
        assertEquals(2, template.stackSize)

        template.clearStack()
        assertEquals(0, template.stackSize)
    }
}
