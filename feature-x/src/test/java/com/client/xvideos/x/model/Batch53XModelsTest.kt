package com.client.xvideos.x.model

import com.client.xvideos.x.extractXVideoIdOrDefault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch53XModelsTest {

    @Test
    fun `ItemsX presence properties`() {
        val empty = ItemsX.EMPTY
        assertFalse(empty.hasTitle)
        assertFalse(empty.hasDuration)
        assertFalse(empty.hasViews)
        assertFalse(empty.hasHref)

        val item = ItemsX(
            id = 42L,
            title = "Video Title",
            duration = "10 min",
            views = "1.2M",
            href = "/video123"
        )
        assertTrue(item.hasTitle)
        assertTrue(item.hasDuration)
        assertTrue(item.hasViews)
        assertTrue(item.hasHref)
    }

    @Test
    fun `ModelScreenTag title properties and fallback`() {
        val empty = ModelScreenTag.EMPTY
        assertFalse(empty.hasTitle0)
        assertFalse(empty.hasTitle1)
        assertEquals("", empty.displayTitle)

        val withTitle0 = ModelScreenTag(title0 = "Main Title", title1 = "Secondary")
        assertTrue(withTitle0.hasTitle0)
        assertTrue(withTitle0.hasTitle1)
        assertEquals("Main Title", withTitle0.displayTitle)

        val withTitle1Only = ModelScreenTag(title0 = "", title1 = "Only Secondary")
        assertFalse(withTitle1Only.hasTitle0)
        assertTrue(withTitle1Only.hasTitle1)
        assertEquals("Only Secondary", withTitle1Only.displayTitle)
    }

    @Test
    fun `XHistoryItem delegated getters`() {
        val item = ItemsX(id = 99L, title = "History Video", previewImage = "https://cdn/p.jpg")
        val history = XHistoryItem(item = item)

        assertEquals(99L, history.id)
        assertEquals("History Video", history.title)
        assertEquals("https://cdn/p.jpg", history.previewImage)
    }

    @Test
    fun `CountryFlag getFlagEmojiOrNull`() {
        assertNull(getFlagEmojiOrNull(null))
        assertNull(getFlagEmojiOrNull("invalid"))
        assertNull(getFlagEmojiOrNull("x"))

        val emoji = getFlagEmojiOrNull("fr")
        assertTrue(emoji != null && emoji != UNKNOWN_FLAG)
    }

    @Test
    fun `HTML5PlayerConfig bestThumbnailUrl and Sponsor helpers`() {
        val configBoth = HTML5PlayerConfig(
            thumbUrl = "https://cdn/standard.jpg",
            thumbUrl169 = "https://cdn/169.jpg"
        )
        assertEquals("https://cdn/169.jpg", configBoth.bestThumbnailUrl)

        val configStandardOnly = HTML5PlayerConfig(
            thumbUrl = "https://cdn/standard.jpg",
            thumbUrl169 = ""
        )
        assertEquals("https://cdn/standard.jpg", configStandardOnly.bestThumbnailUrl)

        val sponsor = Sponsor(link = "https://sponsor.com", name = "Sponsor Name")
        assertTrue(sponsor.hasLink)
        assertTrue(sponsor.hasName)
    }

    @Test
    fun `TagsModel and TagsMainUploaderPornstar helpers`() {
        val uploader = TagsMainUploaderPornstar(href = "/milfed", name = "Milfed", count = "10k")
        assertTrue(uploader.hasHref)
        assertTrue(uploader.hasName)

        val model = TagsModel(tags = listOf("tag1", "tag2"))
        assertTrue(model.hasMultipleTags)

        val singleTag = TagsModel(tags = listOf("tag1"))
        assertFalse(singleTag.hasMultipleTags)
    }

    @Test
    fun `extractXVideoIdOrDefault helper`() {
        assertEquals(12345L, extractXVideoIdOrDefault("/video12345/title"))
        assertEquals(0L, extractXVideoIdOrDefault(null))
        assertEquals(0L, extractXVideoIdOrDefault("invalid_url"))
        assertEquals(999L, extractXVideoIdOrDefault(null, default = 999L))
        assertEquals(999L, extractXVideoIdOrDefault("invalid_url", default = 999L))
    }
}
