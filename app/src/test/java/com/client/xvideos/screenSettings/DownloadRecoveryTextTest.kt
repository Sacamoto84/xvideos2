package com.client.xvideos.screenSettings

import com.client.xvideos.l.featured.saved.LDownloadRecoveryReport
import com.client.xvideos.r.common.downloader.RedDownloadRecoveryReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadRecoveryTextTest {

    @Test
    fun `redDownloadRecoveryText formats according to work state and report stats`() {
        val workingText = redDownloadRecoveryText(null, isWorking = true)
        assertEquals("Сканирование .info и запуск недостающих загрузок", workingText)

        val initialPrompt = redDownloadRecoveryText(null, isWorking = false)
        assertEquals("Если после backup есть только .info, скачает недостающие mp4/jpg", initialPrompt)

        val completeReport = RedDownloadRecoveryReport(
            totalInfoFiles = 15,
            incompleteItems = 0,
        )
        val completeText = redDownloadRecoveryText(completeReport, isWorking = false)
        assertEquals("Все файлы на месте: 15 info", completeText)

        val incompleteReport = RedDownloadRecoveryReport(
            totalInfoFiles = 20,
            incompleteItems = 4,
            queuedVideo = 3,
            queuedPreview = 1,
        )
        val incompleteText = redDownloadRecoveryText(incompleteReport, isWorking = false)
        assertEquals("Найдено 4 из 20 • видео 3 • превью 1", incompleteText)
    }

    @Test
    fun `redDownloadRecoveryConsoleText outputs complete structured log`() {
        val report = RedDownloadRecoveryReport(
            totalInfoFiles = 10,
            incompleteItems = 2,
            queuedVideo = 2,
            queuedPreview = 1,
            skippedNoVideoUrl = 0,
            skippedNoPreviewUrl = 1,
            invalidInfoFiles = 0,
        )
        val console = redDownloadRecoveryConsoleText(report)
        assertTrue(console.contains("R итог"))
        assertTrue(console.contains("всего 10, неполных 2"))
        assertTrue(console.contains("видео 2, preview 1"))
    }

    @Test
    fun `lDownloadRecoveryConsoleText outputs complete structured log`() {
        val report = LDownloadRecoveryReport(
            totalMetadataFiles = 8,
            incompleteItems = 1,
            downloadedMedia = 1,
            downloadedPreview = 0,
            skippedNoMediaUrl = 0,
            skippedNoPreviewUrl = 0,
            failedMedia = 0,
            failedPreview = 0,
            invalidMetadataFiles = 0,
        )
        val console = lDownloadRecoveryConsoleText(report)
        assertTrue(console.contains("L итог"))
        assertTrue(console.contains("всего 8, неполных 1"))
        assertTrue(console.contains("media 1, preview 0"))
    }

    @Test
    fun `shouldAutoRecoverL triggers for L likes and collection paths`() {
        assertTrue(shouldAutoRecoverL(setOf("L")))
        assertTrue(shouldAutoRecoverL(setOf("L/Likes")))
        assertTrue(shouldAutoRecoverL(setOf("L/Likes/fav_album")))
        assertTrue(shouldAutoRecoverL(setOf("L/Collection")))
        assertTrue(shouldAutoRecoverL(setOf("L/Collection/group1")))

        assertFalse(shouldAutoRecoverL(setOf("R")))
        assertFalse(shouldAutoRecoverL(setOf("R/Download")))
        assertFalse(shouldAutoRecoverL(setOf("X")))
        assertFalse(shouldAutoRecoverL(emptySet()))
    }

    @Test
    fun `shouldAutoRecoverRedDownload triggers for R download paths`() {
        assertTrue(shouldAutoRecoverRedDownload(setOf("R")))
        assertTrue(shouldAutoRecoverRedDownload(setOf("R/Download")))
        assertTrue(shouldAutoRecoverRedDownload(setOf("R/Download/item_123")))

        assertFalse(shouldAutoRecoverRedDownload(setOf("R/Likes")))
        assertFalse(shouldAutoRecoverRedDownload(setOf("L")))
        assertFalse(shouldAutoRecoverRedDownload(setOf("L/Likes")))
        assertFalse(shouldAutoRecoverRedDownload(emptySet()))
    }
}
