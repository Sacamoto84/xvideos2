package com.client.xvideos.x

import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.model.XHistoryItem
import com.client.xvideos.x.parcer.hasVideoPreview
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
    }

    @Test
    fun `ModelScreenTag count and firstOrNull`() {
        val empty = ModelScreenTag.EMPTY
        assertEquals(0, empty.count)
        assertNull(empty.firstOrNull)

        val item1 = ItemsX(id = 10L, title = "V1")
        val item2 = ItemsX(id = 20L, title = "V2")
        val model = ModelScreenTag(items = listOf(item1, item2))
        assertEquals(2, model.count)
        assertEquals(item1, model.firstOrNull)
    }

    @Test
    fun `TagsModel and TagsMainUploaderPornstar helpers`() {
        val uploader = TagsMainUploaderPornstar(href = "/uploader/1", name = "Uploader 1")

        val tagsModel = TagsModel(
            mainUploader = listOf(uploader),
            pornstars = listOf(TagsMainUploaderPornstar(href = "/model/1", name = "Model 1")),
            tags = listOf("Amateur", "HD", "POV")
        )
        assertEquals(3, tagsModel.tagsCount)
        assertEquals(1, tagsModel.mainUploaderCount)
        assertEquals(1, tagsModel.pornstarsCount)
        assertEquals(5, tagsModel.totalCount)
    }

    @Test
    fun `parserVideoPreview helpers and existence checks`() {
        val validUrl = "https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg"
        assertTrue(hasVideoPreview(validUrl))

        assertFalse(hasVideoPreview(null))
        assertFalse(hasVideoPreview("https://example.com/not-xvideos/img.jpg"))
    }
}
