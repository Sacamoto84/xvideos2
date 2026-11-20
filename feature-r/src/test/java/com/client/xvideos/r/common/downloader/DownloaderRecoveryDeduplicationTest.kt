package com.client.xvideos.r.common.downloader

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.URL1
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `докачка ждёт отложенный ответ и завершается при сетевой ошибке`() = runBlocking {
        val received = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/preview") { exchange ->
            exchange.use {
                received.countDown()
                release.await(5, TimeUnit.SECONDS)
                exchange.sendResponseHeaders(404, -1)
            }
        }
        server.start()
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            val item = GifsInfo(
                id = "pending",
                userName = "recovery_test",
                urls = URL1(poster = "http://127.0.0.1:${server.address.port}/preview"),
            )
            val directory = File(AppPath.r_cache_download, item.userName).apply { mkdirs() }
            File(directory, "pending.mp4").writeText("готовый файл")
            File(directory, "pending.info").writeText(AppJson.encodeToString(GifsInfo.serializer(), item))
            val red = DownloadRed(downloader, testScope, ContextWrapper(null))
            val recovery = async(Dispatchers.IO) { red.recoverIncompleteDownloads() }

            try {
                assertTrue(received.await(5, TimeUnit.SECONDS))
                assertFalse(recovery.isCompleted)
            } finally {
                release.countDown()
            }
            assertEquals(1, withTimeout(5_000) { recovery.await() }.queuedPreview)
            assertFalse(File(directory, "pending.jpg").exists())
        } finally {
            server.stop(0)
            Dispatchers.resetMain()
        }
    }
}
