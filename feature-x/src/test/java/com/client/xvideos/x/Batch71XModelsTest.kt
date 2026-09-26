package com.client.xvideos.x

import com.client.xvideos.x.model.HTML5PlayerConfig
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.model.UNKNOWN_FLAG
import com.client.xvideos.x.model.XHistoryItem
import com.client.xvideos.x.model.isValidFlagClass
import com.client.xvideos.x.model.toCountryFlagEmojiOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch71XModelsTest {

    @Test
    fun `CountryFlag helpers`() {
        val usEmoji = "us".toCountryFlagEmojiOrNull()
        assertNotNull(usEmoji)
        assertFalse(usEmoji == UNKNOWN_FLAG)

        val nullEmoji: String? = null
        assertNull(nullEmoji.toCountryFlagEmojiOrNull())

        val invalidEmoji = "invalid".toCountryFlagEmojiOrNull()
        assertNull(invalidEmoji)

        assertTrue(isValidFlagClass("flag-us"))
        assertTrue(isValidFlagClass("flag-fr"))
        assertFalse(isValidFlagClass("unknown-class"))
        assertFalse(isValidFlagClass(null))
        assertFalse(isValidFlagClass(""))
    }

    @Test
    fun `HTML5PlayerConfig allVideoUrls, normalizedTitle, and matches`() {
        val config = HTML5PlayerConfig(
            videoTitle = "  Epic 4K Scene  ",
            uploaderName = "StudioPro",
            videoUrlHigh = "https://cdn/high.mp4",
            videoUrlLow = "https://cdn/low.mp4"
        )

        assertEquals("Epic 4K Scene", config.normalizedTitle)
        assertEquals(2, config.allVideoUrls.size)
        assertTrue(config.allVideoUrls.contains("https://cdn/high.mp4"))
        assertTrue(config.allVideoUrls.contains("https://cdn/low.mp4"))

        assertTrue(config.matches("epic"))
        assertTrue(config.matches("studiopro"))
        assertFalse(config.matches("nonexistent"))
        assertTrue(config.matches(null))
        assertTrue(config.matches(""))
    }

    @Test
    fun `ItemsX bestPreviewUrl, hasAnyPreview, and withPreviewImage`() {
        val itemOnlyImg = ItemsX(id = 10L, previewImage = "https://cdn/preview.jpg")
        assertEquals("https://cdn/preview.jpg", itemOnlyImg.bestPreviewUrl)
        assertTrue(itemOnlyImg.hasAnyPreview)

        val itemWithVideo = itemOnlyImg.copy(previewVideo = "https://cdn/preview.mp4")
        assertEquals("https://cdn/preview.mp4", itemWithVideo.bestPreviewUrl)
        assertTrue(itemWithVideo.hasAnyPreview)

        val updatedImg = itemOnlyImg.withPreviewImage("https://cdn/new.jpg")
        assertEquals("https://cdn/new.jpg", updatedImg.previewImage)

        val emptyItem = ItemsX.EMPTY
        assertFalse(emptyItem.hasAnyPreview)
        assertEquals("", emptyItem.bestPreviewUrl)
    }

    @Test
    fun `ModelScreenTag normalizedTitle and matches`() {
        val model = ModelScreenTag(
            title0 = "  Popular Tags  ",
            items = listOf(ItemsX(id = 50L, title = "Action Movie"))
        )

        assertEquals("Popular Tags", model.normalizedTitle)
        assertTrue(model.matches("popular"))
        assertTrue(model.matches("movie"))
        assertFalse(model.matches("comedy"))
        assertTrue(model.matches(null))
    }

    @Test
    fun `TagsModel findUploaderByName and matches`() {
        val uploader = TagsMainUploaderPornstar(href = "/uploader/alex", name = "Alex")
        val model = TagsModel(
            mainUploader = listOf(uploader),
            tags = listOf("4k", "hdr")
        )

        assertEquals(uploader, model.findUploaderByName("Alex"))
        assertEquals(uploader, model.findUploaderByName("alex"))
        assertNull(model.findUploaderByName("Bob"))
        assertNull(model.findUploaderByName(null))

        assertTrue(model.matches("alex"))
        assertTrue(model.matches("hdr"))
        assertFalse(model.matches("vr"))
        assertTrue(model.matches(null))
    }

    @Test
    fun `XHistoryItem withTotalDuration, asReset, and remainingSeconds`() {
        val item = XHistoryItem(
            item = ItemsX(id = 100L, title = "Full Film"),
            lastPositionMs = 30_000L,
            totalDurationMs = 90_000L,
            isCompleted = false
        )

        assertEquals(60L, item.remainingSeconds)

        val updatedDuration = item.withTotalDuration(120_000L)
        assertEquals(120_000L, updatedDuration.totalDurationMs)

        val completed = item.asCompleted()
        assertTrue(completed.isCompleted)

        val reset = completed.asReset()
        assertFalse(reset.isCompleted)
        assertEquals(0L, reset.lastPositionMs)
    }

    @Test
    fun `XSite formatDurationSeconds and isCanonicalXLink`() {
        assertEquals("01:15", formatDurationSeconds(75L))
        assertEquals("00:00", formatDurationSeconds(0L))
        assertEquals("1:00:05", formatDurationSeconds(3605L))

        assertTrue("https://www.xv-ru.com/video123".isCanonicalXLink())
        assertFalse("https://external.com/video123".isCanonicalXLink())
    }
}
