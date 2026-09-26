package com.client.xvideos.r.model

import com.client.xvideos.r.model.tag.TagInfo
import com.client.xvideos.r.model.tag.TagSuggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch70RModelsTest {

    @Test
    fun `GifsInfo dimensions, aspectRatio, matches, and formatDuration`() {
        val gif = GifsInfo(
            id = "cool-cat-123",
            width = 1920,
            height = 1080,
            userName = "cat_creator",
            description = "Feline jumping around",
            tags = listOf("cat", "funny"),
            niches = listOf("pets"),
            duration = 75.5
        )

        assertTrue(gif.hasDimensions)
        assertEquals(1920f / 1080f, gif.aspectRatio, 0.001f)
        assertEquals("1:15", gif.formatDuration())

        assertTrue(gif.matches("cool-cat"))
        assertTrue(gif.matches("CAT_CREATOR"))
        assertTrue(gif.matches("feline"))
        assertTrue(gif.matches("funny"))
        assertTrue(gif.matches("pets"))
        assertFalse(gif.matches("dog"))
        assertFalse(gif.matches(null))
        assertFalse(gif.matches(""))

        val shortGif = gif.copy(duration = 9.2)
        assertEquals("9s", shortGif.formatDuration())

        val zeroDurationGif = GifsInfo.EMPTY
        assertEquals("", zeroDurationGif.formatDuration())
        assertEquals(1f, zeroDurationGif.aspectRatio, 0.001f)
        assertTrue(zeroDurationGif.hasDimensions)

        val zeroDimensionsGif = GifsInfo(width = 0, height = 0)
        assertFalse(zeroDimensionsGif.hasDimensions)
    }

    @Test
    fun `CreatorResponse and TopCreator lookup and matching`() {
        val gif1 = GifsInfo(id = "g1")
        val gif2 = GifsInfo(id = "g2")
        val niche1 = NichesInfo(id = "n1", name = "Anime")

        val response = CreatorResponse(
            gifs = listOf(gif1, gif2),
            niches = listOf(niche1)
        )

        assertEquals(gif1, response.findGifByIdOrNull("g1"))
        assertEquals(gif2, response.findGifByIdOrNull("g2"))
        assertNull(response.findGifByIdOrNull("g3"))
        assertNull(response.findGifByIdOrNull(null))

        assertEquals(niche1, response.findNicheByNameOrNull("Anime"))
        assertEquals(niche1, response.findNicheByNameOrNull("ANIME"))
        assertNull(response.findNicheByNameOrNull("Cosplay"))
        assertNull(response.findNicheByNameOrNull(null))

        val creator = TopCreator(
            username = "star_creator",
            name = "Star Master",
            description = "Top content producer"
        )
        assertTrue(creator.matches("star_creator"))
        assertTrue(creator.matches("master"))
        assertTrue(creator.matches("producer"))
        assertFalse(creator.matches("unknown"))
        assertFalse(creator.matches(null))
        assertFalse(creator.matches(""))
    }

    @Test
    fun `MediaResponse totalGifsCount and filters`() {
        val gifA = GifsInfo(id = "a", description = "Alpha wolf")
        val gifB = GifsInfo(id = "b", description = "Beta tester")
        val response = MediaResponse(
            total = 42,
            gifs = listOf(gifA, gifB)
        )

        assertEquals(42, response.totalGifsCount)
        assertEquals(gifA, response.findGifByIdOrNull("a"))
        assertNull(response.findGifByIdOrNull("c"))
        assertNull(response.findGifByIdOrNull(null))

        val filteredAlpha = response.filterGifsByQuery("wolf")
        assertEquals(1, filteredAlpha.size)
        assertEquals("a", filteredAlpha[0].id)

        val allGifs = response.filterGifsByQuery(null)
        assertEquals(2, allGifs.size)
    }

    @Test
    fun `NichesInfo and NichesResponse Niche helpers`() {
        val nicheInfo = NichesInfo(id = "  BIG-areolas  ", name = "Big Areolas")
        assertEquals("big-areolas", nicheInfo.normalizedId)

        val niche = Niche(
            id = "petite",
            name = "Petite Girls",
            previews = listOf(Preview(id = "p1"), Preview(id = "p2"))
        )
        assertEquals(2, niche.previewsCount)
        assertTrue(niche.matches("petite"))
        assertTrue(niche.matches("GIRLS"))
        assertFalse(niche.matches("tall"))
        assertFalse(niche.matches(null))
        assertFalse(niche.matches(""))
    }

    @Test
    fun `Order and MediaType cyclic navigation and fromOrdinalOrDefault`() {
        // Order navigation
        val firstOrder = Order.entries.first()
        val lastOrder = Order.entries.last()

        assertEquals(Order.entries[1], firstOrder.next())
        assertEquals(firstOrder, lastOrder.next())
        assertEquals(lastOrder, firstOrder.prev())

        assertEquals(Order.TRENDING, Order.fromOrdinalOrDefault(0))
        assertEquals(Order.DEFAULT, Order.fromOrdinalOrDefault(999))

        // MediaType navigation
        assertEquals(MediaType.GIF, MediaType.IMAGE.next())
        assertEquals(MediaType.ALL, MediaType.GIF.next())
        assertEquals(MediaType.IMAGE, MediaType.ALL.next())

        assertEquals(MediaType.ALL, MediaType.IMAGE.prev())
        assertEquals(MediaType.GIF, MediaType.ALL.prev())

        assertEquals(MediaType.IMAGE, MediaType.fromOrdinalOrDefault(0))
        assertEquals(MediaType.DEFAULT, MediaType.fromOrdinalOrDefault(999))
    }

    @Test
    fun `URL1 hasValidVideo and hasValidImage`() {
        val validUrl = URL1(
            sd = "https://cdn/video.mp4",
            poster = "https://cdn/poster.jpg"
        )
        assertTrue(validUrl.hasValidVideo)
        assertTrue(validUrl.hasValidImage)

        val emptyUrl = URL1.EMPTY
        assertFalse(emptyUrl.hasValidVideo)
        assertFalse(emptyUrl.hasValidImage)
    }

    @Test
    fun `UserInfo formatFollowers`() {
        val userSmall = UserInfo(followers = 500)
        assertEquals("500", userSmall.formatFollowers())

        val userThousand = UserInfo(followers = 12_500)
        assertEquals("12.5k", userThousand.formatFollowers())

        val userMillion = UserInfo(followers = 3_400_000)
        assertEquals("3.4M", userMillion.formatFollowers())
    }

    @Test
    fun `TagInfo and TagSuggestion matchesQuery and hasCount`() {
        val tag = TagInfo(name = "Cosplay", count = 100)
        assertTrue(tag.matchesQuery(null))
        assertTrue(tag.matchesQuery(""))
        assertTrue(tag.matchesQuery("cosplay"))
        assertFalse(tag.matchesQuery("anime"))

        val suggestion = TagSuggestion(text = "Blonde", gifs = 250, type = "tag")
        assertTrue(suggestion.hasCount)
        assertTrue(suggestion.matchesQuery(null))
        assertTrue(suggestion.matchesQuery(""))
        assertTrue(suggestion.matchesQuery("blonde"))
        assertFalse(suggestion.matchesQuery("redhead"))

        val zeroSuggestion = TagSuggestion()
        assertFalse(zeroSuggestion.hasCount)
    }
}
