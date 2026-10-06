package com.client.xvideos.x

import com.client.xvideos.x.model.HTML5PlayerConfig
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.model.XHistoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch71XModelsTest {

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
    fun `ItemsX bestPreviewUrl and hasAnyPreview`() {
        val itemOnlyImg = ItemsX(id = 10L, previewImage = "https://cdn/preview.jpg")
        assertEquals("https://cdn/preview.jpg", itemOnlyImg.bestPreviewUrl)
        assertTrue(itemOnlyImg.hasAnyPreview)

        val itemWithVideo = itemOnlyImg.copy(previewVideo = "https://cdn/preview.mp4")
        assertEquals("https://cdn/preview.mp4", itemWithVideo.bestPreviewUrl)
        assertTrue(itemWithVideo.hasAnyPreview)

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
    fun `TagsModel matches`() {
        val uploader = TagsMainUploaderPornstar(href = "/uploader/alex", name = "Alex")
        val model = TagsModel(
            mainUploader = listOf(uploader),
            tags = listOf("4k", "hdr")
        )

        assertTrue(model.matches("alex"))
        assertTrue(model.matches("hdr"))
        assertFalse(model.matches("vr"))
        assertTrue(model.matches(null))
    }

    @Test
    fun `XHistoryItem remainingSeconds`() {
        val item = XHistoryItem(
            item = ItemsX(id = 100L, title = "Full Film"),
            lastPositionMs = 30_000L,
            totalDurationMs = 90_000L,
            isCompleted = false
        )

        assertEquals(60L, item.remainingSeconds)
    }

}
