package com.client.xvideos.x

import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.model.UNKNOWN_FLAG
import com.client.xvideos.x.model.XHistoryItem
import com.client.xvideos.x.model.getFlagEmojiOrDefault
import com.client.xvideos.x.model.toCountryFlagEmoji
import com.client.xvideos.x.parcer.hasVideoPreview
import com.client.xvideos.x.parcer.hasVideoTags
import com.client.xvideos.x.parcer.parserItemVideoTagsOrEmpty
import com.client.xvideos.x.parcer.parserVideoPreviewOrDefault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch60XModelsAndParsersTest {

    @Test
    fun `ItemsX profile display name and video identity helpers`() {
        val itemWithName = ItemsX(id = 101L, nameProfile = "Profile Name", channel = "Channel Name", href = "/video101", title = "Video 101")
        assertEquals("Profile Name", itemWithName.displayNameProfile)
        assertTrue(itemWithName.hasValidHref)
        assertTrue(itemWithName.hasValidTitle)

        val itemWithChannelOnly = ItemsX(id = 102L, nameProfile = "", channel = "Fallback Channel")
        assertEquals("Fallback Channel", itemWithChannelOnly.displayNameProfile)

        val sameItem = ItemsX(id = 101L, title = "Different Title")
        assertTrue(itemWithName.isSameVideo(sameItem))

        val differentItem = ItemsX(id = 999L)
        assertFalse(itemWithName.isSameVideo(differentItem))
        assertFalse(itemWithName.isSameVideo(null))
        assertFalse(ItemsX.EMPTY.isSameVideo(ItemsX.EMPTY))
    }

    @Test
    fun `XHistoryItem inspection and comparison helpers`() {
        val historyItem = XHistoryItem(
            item = ItemsX(id = 200L, title = "History Title", previewImage = "https://cdn/img.jpg"),
            updatedAt = 123456789L
        )
        assertTrue(historyItem.hasTitle)
        assertTrue(historyItem.hasPreview)
        assertTrue(historyItem.hasUpdatedAt)

        val sameHistory = XHistoryItem(item = ItemsX(id = 200L))
        assertTrue(historyItem.isSameItem(sameHistory))
        assertFalse(historyItem.isSameItem(XHistoryItem(item = ItemsX(id = 300L))))
        assertFalse(historyItem.isSameItem(null))
    }

    @Test
    fun `ModelScreenTag count and findByIdOrNull`() {
        val empty = ModelScreenTag.EMPTY
        assertEquals(0, empty.count)
        assertNull(empty.firstOrNull)
        assertNull(empty.findByIdOrNull(10L))

        val item1 = ItemsX(id = 10L, title = "V1")
        val item2 = ItemsX(id = 20L, title = "V2")
        val model = ModelScreenTag(items = listOf(item1, item2))
        assertEquals(2, model.count)
        assertEquals(item1, model.firstOrNull)
        assertEquals(item2, model.findByIdOrNull(20L))
        assertNull(model.findByIdOrNull(99L))
        assertNull(model.findByIdOrNull(0L))
        assertNull(model.findByIdOrNull(-1L))
    }

    @Test
    fun `TagsModel and TagsMainUploaderPornstar helpers`() {
        val uploader = TagsMainUploaderPornstar(href = "/uploader/1", name = "Uploader 1")
        val sameUploader = TagsMainUploaderPornstar(href = "/uploader/1", name = "Different Name")
        assertTrue(uploader.isSame(sameUploader))
        assertFalse(uploader.isSame(TagsMainUploaderPornstar(href = "/uploader/2", name = "Uploader 2")))
        assertFalse(uploader.isSame(null))

        val tagsModel = TagsModel(
            mainUploader = listOf(uploader),
            pornstars = listOf(TagsMainUploaderPornstar(href = "/model/1", name = "Model 1")),
            tags = listOf("Amateur", "HD", "POV")
        )
        assertEquals(3, tagsModel.tagsCount)
        assertEquals(1, tagsModel.mainUploaderCount)
        assertEquals(1, tagsModel.pornstarsCount)
        assertEquals(5, tagsModel.totalCount)

        assertTrue(tagsModel.containsTag("amateur"))
        assertTrue(tagsModel.containsTag("HD"))
        assertFalse(tagsModel.containsTag("4K"))
        assertFalse(tagsModel.containsTag(null))
        assertFalse(tagsModel.containsTag(""))
    }

    @Test
    fun `CountryFlag helpers and extension functions`() {
        assertEquals("🇺🇸", getFlagEmojiOrDefault("us"))
        assertEquals("🇫🇷", getFlagEmojiOrDefault("flag-fr"))
        assertEquals("🌍", getFlagEmojiOrDefault("invalid-country", default = "🌍"))
        assertEquals(UNKNOWN_FLAG, getFlagEmojiOrDefault(null))

        assertEquals("🇩🇪", "de".toCountryFlagEmoji())
        assertEquals(UNKNOWN_FLAG, (null as String?).toCountryFlagEmoji())
    }

    @Test
    fun `parserItemVideoTags null-safe overloads and presence checks`() {
        assertEquals(TagsModel.EMPTY, parserItemVideoTagsOrEmpty(null))
        assertEquals(TagsModel.EMPTY, parserItemVideoTagsOrEmpty(""))
        assertEquals(TagsModel.EMPTY, parserItemVideoTagsOrEmpty("   "))

        assertFalse(hasVideoTags(null))
        assertFalse(hasVideoTags(""))
        assertFalse(hasVideoTags("<div>just some plain text</div>"))
        assertTrue(hasVideoTags("<ul><li class=\"main-uploader\"><a href=\"/u\">User</a></li></ul>"))
        assertTrue(hasVideoTags("<ul><li><a class=\"is-keyword\">Tag</a></li></ul>"))
    }

    @Test
    fun `parserVideoPreview helpers and existence checks`() {
        val validUrl = "https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg"
        assertTrue(hasVideoPreview(validUrl))
        val parsed = parserVideoPreviewOrDefault(validUrl)
        assertTrue(parsed.isNotEmpty())
        assertTrue(parsed.contains("videopreview"))

        assertFalse(hasVideoPreview(null))
        assertFalse(hasVideoPreview("https://example.com/not-xvideos/img.jpg"))
        assertEquals("fallback_url", parserVideoPreviewOrDefault(null, default = "fallback_url"))
    }
}
