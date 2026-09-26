package com.client.xvideos.x

import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.parcer.countTotalParsedTags
import com.client.xvideos.x.parcer.extractPrimaryStreamUrl
import com.client.xvideos.x.parcer.hasModelsOrPornstars
import com.client.xvideos.x.parcer.hasMp4Extension
import com.client.xvideos.x.parcer.isHighQualityPreview
import com.client.xvideos.x.parcer.parseFirstVideoIdOrNull
import com.client.xvideos.x.search.isSuggestUrl
import com.client.xvideos.x.search.isValidSuggestQuery
import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Keyword
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult
import com.client.xvideos.x.search.parseJsonChannels
import com.client.xvideos.x.search.parseJsonPornstars
import com.client.xvideos.x.search.parseJsonTotalCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch75XSearchAndParsersTest {

    @Test
    fun `Keyword validation and mutation`() {
        val keywordValid = Keyword(N = "blonde", R = "4.8")
        assertTrue(keywordValid.isValidRating)
        assertEquals(4.8, keywordValid.ratingDoubleOrNull ?: 0.0, 0.001)

        val keywordInvalid = Keyword(N = "brunette", R = "abc")
        assertFalse(keywordInvalid.isValidRating)
        assertNull(keywordInvalid.ratingDoubleOrNull)

        val mutated = keywordInvalid.withRating("5.0")
        assertTrue(mutated.isValidRating)
        assertEquals("5.0", mutated.rating)
    }

    @Test
    fun `Pornstar profileUrl and formatters`() {
        val star = Pornstar(
            N = "Eva Elfie",
            F = "profiles/eva-elfie",
            T = "pornstar",
            MV = 1500,
            M = 0,
            L = 0,
            P = "https://cdn.example.com/eva.jpg",
            RF = "450k"
        )
        assertEquals("https://www.xvideos.com/profiles/eva-elfie", star.profileUrl)
        assertTrue(star.hasValidSubscribers)
        assertEquals("1.5k", star.formatVideos())

        val smallStar = star.copy(MV = 42, RF = "0")
        assertEquals("42", smallStar.formatVideos())
        assertFalse(smallStar.hasValidSubscribers)
    }

    @Test
    fun `Channel profileUrl and verification`() {
        val channel = Channel(
            N = "Brazzers",
            F = "profiles/brazzers",
            T = "channel",
            CPV = true,
            M = 0,
            L = 0,
            P = "https://cdn.example.com/logo.jpg",
            RF = "1.2M"
        )
        assertTrue(channel.isVerified)
        assertEquals("https://www.xvideos.com/profiles/brazzers", channel.profileUrl)
    }

    @Test
    fun `SearchResult filtering and suggestions list`() {
        val result = SearchResult(
            result = true,
            code = 200,
            keywords = listOf(Keyword(N = "cosplay", R = "1"), Keyword(N = "anime", R = "2")),
            pornstar = listOf(Pornstar(N = "Sweetie Fox", F = "profiles/sweetie", T = "pornstar", MV = 10, M = 0, L = 0, P = "", RF = "")),
            channel = listOf(Channel(N = "Sweetie Studio", F = "profiles/studio", T = "channel", CPV = false, M = 0, L = 0, P = "", RF = ""))
        )

        assertEquals(listOf("cosplay", "anime", "Sweetie Fox", "Sweetie Studio"), result.allSuggestions)

        val filtered = result.filterByQuery("sweetie")
        assertEquals(0, filtered.keywords.size)
        assertEquals(1, filtered.pornstar?.size)
        assertEquals(1, filtered.channel?.size)
        assertEquals("Sweetie Fox", filtered.pornstar?.first()?.name)
    }

    @Test
    fun `parseJson helper extractions`() {
        val json = """
            {
               "result": true,
               "code": 200,
               "keywords": [{"N": "asian", "R": "9"}],
               "pornstar": [{"N": "Rae Lil Black", "F": "/profiles/rae", "T": "pornstar", "MV": 200, "M": 0, "L": 0, "P": "", "RF": "100k"}],
               "channel": [{"N": "Asian Street", "F": "/profiles/street", "T": "channel", "CPV": true, "M": 0, "L": 0, "P": "", "RF": "50k"}]
            }
        """.trimIndent()

        val stars = parseJsonPornstars(json)
        assertEquals(1, stars.size)
        assertEquals("Rae Lil Black", stars.first().name)

        val channels = parseJsonChannels(json)
        assertEquals(1, channels.size)
        assertEquals("Asian Street", channels.first().name)

        assertEquals(3, parseJsonTotalCount(json))
    }

    @Test
    fun `Search URL and query predicates`() {
        assertTrue(isValidSuggestQuery("milf"))
        assertFalse(isValidSuggestQuery("   "))
        assertFalse(isValidSuggestQuery(null))

        assertTrue(isSuggestUrl("https://www.xvideos.com/search-suggest/teen"))
        assertFalse(isSuggestUrl("https://www.xvideos.com/video123"))
        assertFalse(isSuggestUrl(null))
    }

    @Test
    fun `Video preview and extension helpers`() {
        assertTrue(isHighQualityPreview("https://cdn.example.com/video_169.mp4"))
        assertFalse(isHighQualityPreview("https://cdn.example.com/video.mp4"))

        assertTrue(hasMp4Extension("https://cdn.example.com/file.mp4?token=123"))
        assertFalse(hasMp4Extension("https://cdn.example.com/file.jpg"))
    }

    @Test
    fun `Tags and stream extraction helpers`() {
        val model = TagsModel(
            mainUploader = listOf(TagsMainUploaderPornstar(href = "/uploader", name = "Channel", count = "1")),
            pornstars = listOf(TagsMainUploaderPornstar(href = "/model", name = "Star", count = "2")),
            tags = listOf("tag1", "tag2")
        )
        assertEquals(4, countTotalParsedTags(model))

        assertTrue(hasModelsOrPornstars("""<li class="main-uploader"><span>Uploader</span></li>"""))
        assertFalse(hasModelsOrPornstars("""<div class="regular-block">Text</div>"""))

        val html = """<div class="frame-block" data-id="987654"></div>"""
        assertEquals(987654L, parseFirstVideoIdOrNull(html))

        val script = """
            html5player.setVideoUrlHigh('https:\/\/cdn.example.com\/high.mp4');
            html5player.setVideoUrlLow('https:\/\/cdn.example.com\/low.mp4');
        """.trimIndent()
        val primary = extractPrimaryStreamUrl(script)
        assertNotNull(primary)
        assertTrue(primary.contains("high.mp4"))
    }
}
