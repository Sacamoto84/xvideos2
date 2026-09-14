package com.client.xvideos.r.common.downloader

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.r.model.GifsInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DownloaderRecoveryDeduplicationTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_r").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloader = Downloader(
        kDownloader = KDownloader.createForTesting(),
        scope = testScope
    )

    @Test
    fun findVideoInDownload_rejects_unsafe_and_blank_names() {
        assertFalse(downloader.findVideoInDownload("", ""))
        assertFalse(downloader.findVideoInDownload("id123", ""))
        assertFalse(downloader.findVideoInDownload("", "creator"))
        assertFalse(downloader.findVideoInDownload("..", "creator"))
        assertFalse(downloader.findVideoInDownload("id123", "../creator"))
        assertFalse(downloader.findVideoInDownload("id/123", "creator"))
    }

    @Test
    fun downloadMissingFiles_and_recovery_reject_unsafe_items_safely() {
        val blankItem = GifsInfo(id = "", userName = "")
        val unsafeItem = GifsInfo(id = "../traversal", userName = "legit_user")

        val reportBlank = downloader.downloadMissingFiles(blankItem)
        val reportBlankRecovery = downloader.downloadMissingFilesForRecovery(blankItem)
        assertEquals(0, reportBlank.queuedVideo)
        assertEquals(0, reportBlankRecovery.queuedVideo)

        val reportUnsafe = downloader.downloadMissingFiles(unsafeItem)
        val reportUnsafeRecovery = downloader.downloadMissingFilesForRecovery(unsafeItem)
        assertEquals(0, reportUnsafe.queuedVideo)
        assertEquals(0, reportUnsafeRecovery.queuedVideo)
    }
}
