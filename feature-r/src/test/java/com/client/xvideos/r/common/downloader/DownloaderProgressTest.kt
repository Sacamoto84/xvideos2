package com.client.xvideos.r.common.downloader

import com.client.xvideos.common.kdownloader.KDownloader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloaderProgressTest {

    private fun downloader() = Downloader(KDownloader.createForTesting())

    @Test
    fun `параллельные загрузки дают общий прогресс, окончание одной не выставляет простой`() {
        val downloader = downloader()

        downloader.onVideoProgress("a", 0.2f)
        downloader.onVideoProgress("b", 0.6f)
        assertEquals(0.4f, downloader.percent.value, 0.0001f)

        downloader.onVideoFinished("a", failed = false)
        assertTrue(downloader.isDownloading())
        assertEquals(0.6f, downloader.percent.value, 0.0001f)

        downloader.onVideoFinished("b", failed = false)
        assertTrue(downloader.isIdle())
    }

    @Test
    fun `ошибка последней загрузки выставляет ошибку, а при активных — прогресс остальных`() {
        val downloader = downloader()

        downloader.onVideoProgress("a", 0.5f)
        downloader.onVideoProgress("b", 0.1f)
        downloader.onVideoFinished("b", failed = true)
        assertEquals(0.5f, downloader.percent.value, 0.0001f)

        downloader.onVideoFinished("a", failed = true)
        assertTrue(downloader.hasDownloadError())
    }

    @Test
    fun `отказ до постановки в очередь не сбивает идущую загрузку`() {
        val downloader = downloader()

        downloader.onVideoProgress("a", 0.3f)
        downloader.setIdleState(-3f)
        downloader.setIdleState(-2f)
        assertEquals(0.3f, downloader.percent.value, 0.0001f)

        downloader.onVideoFinished("a", failed = false)
        downloader.setIdleState(-3f)
        assertTrue(downloader.hasDownloadError())
    }
}
