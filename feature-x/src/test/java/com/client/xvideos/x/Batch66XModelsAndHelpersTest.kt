package com.client.xvideos.x

import com.client.xvideos.x.model.HTML5PlayerConfig
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import com.client.xvideos.x.model.Sponsor
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.model.XHistoryItem
import com.client.xvideos.x.model.getCountryCodeFromFlagClass
import com.client.xvideos.x.model.isIsoCountryCode
import com.client.xvideos.x.model.normalizeCountryCode
import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Keyword
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch66XModelsAndHelpersTest {

    @Test
    fun `XSite helpers format duration and parse slugs`() {
        assertEquals("00:00", formatDurationMs(0L))
        assertEquals("00:05", formatDurationMs(5_000L))
        assertEquals("01:08", formatDurationMs(68_000L))
        assertEquals("1:00:00", formatDurationMs(3600_000L))
        assertEquals("1:15:30", formatDurationMs(4530_000L))

        assertEquals(68L, parseDurationToSeconds("1:08"))
        assertEquals(600L, parseDurationToSeconds("10 min"))

        assertEquals("uicfdab07bd", extractXVideoSlug("/video.uicfdab07bd/_"))
        assertEquals("12345", extractXVideoSlug("/video12345/test"))
        assertNull(extractXVideoSlug("https://example.com/other"))

        assertTrue(isCanonicalXUrl("https://www.xv-ru.com/video123"))
        assertFalse(isCanonicalXUrl("https://another.com/video123"))

        assertTrue("https://www.xv-ru.com/video123".isXVideoLink())
        assertFalse("https://www.xv-ru.com/tags/".isXVideoLink())
    }

    @Test
    fun `ItemsX model extensions and mutations`() {
        val item = ItemsX(
            id = 42L,
            title = " Great Video ",
            channel = " StudioX ",
            nameProfile = "ProfileName",
            linkProfile = "/profiles/studiox",
            duration = "10:00",
            views = "1.5M",
            href = "/video.42/great_video"
        )

        assertEquals("Great Video", item.normalizedTitle)
        assertEquals("StudioX", item.normalizedChannel)
        assertEquals("studiox", item.cleanProfileLink)

        assertTrue(item.matches("great"))
        assertTrue(item.matches("studio"))
        assertTrue(item.matches("profilename"))
        assertFalse(item.matches("nonexistent"))
        assertTrue(item.matches(null))

        val mutated = item.withDuration("12:30").withViews("2.0M").withHref("/video.42/new")
        assertEquals("12:30", mutated.duration)
        assertEquals("2.0M", mutated.views)
        assertEquals("/video.42/new", mutated.href)
    }

    @Test
    fun `HTML5PlayerConfig media checks and Sponsor helpers`() {
        val config = HTML5PlayerConfig(
            videoTitle = "Player Test",
            thumbUrl = "https://example.com/thumb.jpg"
        )

        assertTrue(config.hasValidTitle)
        assertTrue(config.hasAnyMedia)

        val withStreams = config.withVideoUrls(
            high = "https://example.com/high.mp4",
            low = "https://example.com/low.mp4",
            hls = "https://example.com/hls.m3u8"
        )
        assertTrue(withStreams.hasHighQuality)
        assertTrue(withStreams.hasLowQuality)
        assertTrue(withStreams.hasHls)
        assertEquals("https://example.com/high.mp4", withStreams.bestVideoUrl)

        val sponsor = Sponsor(name = "SponsorBrand", desc = "Best sponsor ever")
        assertEquals("SponsorBrand", sponsor.displayName)
        assertTrue(sponsor.matches("brand"))
        assertTrue(sponsor.matches("ever"))
        assertFalse(sponsor.matches("unknown"))

        val unnamedSponsor = Sponsor(name = "", desc = "Only Description")
        assertEquals("Only Description", unnamedSponsor.displayName)
    }

    @Test
    fun `CountryFlag code normalizers and validators`() {
        assertEquals("us", normalizeCountryCode("us"))
        assertEquals("fr", normalizeCountryCode("flag-fr"))
        assertEquals("de", normalizeCountryCode("  FLAG-DE  "))
        assertEquals("", normalizeCountryCode("invalid-long"))
        assertEquals("", normalizeCountryCode(null))

        assertTrue(isIsoCountryCode("us"))
        assertTrue(isIsoCountryCode("flag-it"))
        assertFalse(isIsoCountryCode("12"))
        assertFalse(isIsoCountryCode(""))

        assertEquals("jp", getCountryCodeFromFlagClass("flag-jp"))
        assertNull(getCountryCodeFromFlagClass("nonflag"))
    }

    @Test
    fun `ModelScreenTag and TagsModel helpers and queries`() {
        val item1 = ItemsX(id = 1L, title = "Alpha Video")
        val item2 = ItemsX(id = 2L, title = "Beta Video")

        val screenTag = ModelScreenTag(
            title0 = "Main Tag",
            items = listOf(item1, item2),
            lastPage = 3
        )

        assertTrue(screenTag.hasPagination)
        assertTrue(screenTag.hasItemWithId(1L))
        assertFalse(screenTag.hasItemWithId(99L))

        val filtered = screenTag.filterByQuery("alpha")
        assertEquals(1, filtered.size)
        assertEquals(1L, filtered.first().id)

        val star = TagsMainUploaderPornstar(
            name = "Model Star",
            href = "/pornstars/model-star",
            count = "50"
        )
        assertEquals("model star", star.normalizedName)
        assertEquals("model-star", star.cleanHref)
        assertTrue(star.matches("model"))

        val tagsModel = TagsModel(
            mainUploader = listOf(TagsMainUploaderPornstar(name = "Uploader")),
            pornstars = listOf(star),
            tags = listOf("Amateur", "HD", "Verified")
        )

        assertEquals(listOf("Uploader", "Model Star"), tagsModel.allNames)
        assertEquals(listOf("Amateur"), tagsModel.filterTags("amat"))
        assertNotNull(tagsModel.findPornstarByName("Model Star"))
        assertNull(tagsModel.findPornstarByName("NonExistent"))
    }

    @Test
    fun `XHistoryItem progress and mutators`() {
        val item = ItemsX(id = 100L, title = "History Item")
        val history = XHistoryItem(
            item = item,
            lastPositionMs = 60_000L,
            totalDurationMs = 120_000L
        )

        assertEquals(50, history.progressPercent)
        assertTrue(history.matches("History"))

        val updated = history.withPosition(90_000L, 123456L)
        assertEquals(90_000L, updated.lastPositionMs)
        assertEquals(123456L, updated.updatedAt)

        val completed = history.asCompleted(999999L)
        assertTrue(completed.isCompleted)
        assertEquals(120_000L, completed.lastPositionMs)
        assertEquals(999999L, completed.updatedAt)
    }

    @Test
    fun `searchModel suggestions and lookups`() {
        val kw = Keyword(N = " Brunette ", R = "100")
        assertEquals("brunette", kw.normalizedName)
        assertTrue(kw.matches("brune"))

        val ps = Pornstar(
            N = "Star Name",
            F = "/profiles/star",
            T = "pornstar",
            MV = 10,
            M = 0,
            L = 0,
            P = "https://example.com/avatar.jpg",
            RF = "1000"
        )
        assertEquals("star name", ps.normalizedName)
        assertEquals("profiles/star", ps.cleanProfilePath)
        assertTrue(ps.matches("name"))

        val ch = Channel(
            N = "Best Channel",
            F = "/profiles/best",
            T = "channel",
            CPV = true,
            M = 0,
            L = 0,
            P = "https://example.com/channel.jpg",
            RF = "5000"
        )
        assertEquals("best channel", ch.normalizedName)
        assertEquals("profiles/best", ch.cleanProfilePath)
        assertTrue(ch.matches("channel"))

        val searchResult = SearchResult(
            keywords = listOf(kw),
            pornstar = listOf(ps),
            channel = listOf(ch)
        )

        assertEquals(listOf(" Brunette ", "Star Name", "Best Channel"), searchResult.allSuggestionNames())
        assertNotNull(searchResult.findPornstarByName("Star Name"))
        assertNull(searchResult.findPornstarByName("Random"))
        assertNotNull(searchResult.findChannelByName("Best Channel"))
        assertNull(searchResult.findChannelByName("Random"))
    }
}
