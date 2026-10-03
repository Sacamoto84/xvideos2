package com.client.xvideos.r.common.downloader

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.URL1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Отмена загрузки — не её сбой. Ролик удаляют, пока он качается: индикатор
 * обязан вернуться в покой, а не показать ошибку.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloaderCancelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val serverSocket = ServerSocket(0)
    private val releaseServer = CountDownLatch(1)

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_r_cancel")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        // Колбэки загрузчика идут через главный диспетчер; в тесте он выполняет их на месте.
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        releaseServer.countDown()
        serverSocket.close()
        Dispatchers.resetMain()
    }

    /** Сервер отдаёт заголовки и первые байты, после чего замолкает: загрузка зависает в чтении. */
    private fun startStallingServer() = thread(isDaemon = true) {
        runCatching {
            serverSocket.accept().use { socket ->
                val input = socket.getInputStream().bufferedReader()
                while (!input.readLine().isNullOrEmpty()) Unit
                socket.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Length: 1000000\r\nContent-Type: video/mp4\r\n\r\n".toByteArray())
                    write(ByteArray(100))
                    flush()
                }
                releaseServer.await(30, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun `отмена качающегося ролика возвращает индикатор в покой, а не в ошибку`() {
        startStallingServer()
        val downloader = Downloader(KDownloader.createForTesting())
        val item = GifsInfo(
            id = "clip1",
            userName = "creator",
            urls = URL1.EMPTY.copy(sd = "http://127.0.0.1:${serverSocket.localPort}/clip1.mp4"),
        )

        downloader.downloadRedName(item)
        val deadline = System.currentTimeMillis() + 5_000
        while (!downloader.isDownloading() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertTrue("загрузка не началась", downloader.isDownloading())

        // Так отменяет загрузку удаление ролика: по тегу, равному id.
        downloader.kDownloader.cancel(item.id)

        assertFalse("отмена показана как ошибка загрузки", downloader.hasDownloadError())
        assertTrue(downloader.isIdle())
    }
}
