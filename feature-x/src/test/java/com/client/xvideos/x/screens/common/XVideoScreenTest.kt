package com.client.xvideos.x.screens.common

import com.client.xvideos.x.feature.saved.SavedLocalVideo
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Удержание и двойной тап по карточке: сохранённый ролик открывается
 * скачанным файлом, несохранённый — из сети (спека
 * 2026-10-22-x-saved-card-local-preview-design.md).
 */
class XVideoScreenTest {

    private val item = ItemsX(id = 5L, href = "/video5/x")

    @Test
    fun `сохранённый ролик открывается скачанным файлом`() {
        val screen = xVideoScreen(item, SavedLocalVideo(fileUrl = "file:///d/5.mp4", posterPath = null))

        screen as ScreenX_LocalVideoPlayer
        assertEquals("file:///d/5.mp4", screen.fileUrl)
        assertSame(item, screen.item)
    }

    @Test
    fun `несохранённый ролик открывается из сети`() {
        val screen = xVideoScreen(item, null)

        screen as ScreenX_VideoPlayer
        assertEquals(normalizeXUrl(item.href), screen.url)
    }
}
