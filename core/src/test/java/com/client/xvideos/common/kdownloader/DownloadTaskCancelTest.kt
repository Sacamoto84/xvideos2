package com.client.xvideos.common.kdownloader

import com.client.xvideos.common.kdownloader.database.NoOpsDbHelper
import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import com.client.xvideos.common.kdownloader.internal.DownloadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Отмена загрузки обязана оборвать блокирующее чтение сразу, а не ждать, пока
 * истечёт таймаут чтения на зависшем соединении.
 */
class DownloadTaskCancelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val serverSocket = ServerSocket(0)
    private val releaseServer = CountDownLatch(1)

    @After
    fun tearDown() {
        releaseServer.countDown()
        serverSocket.close()
    }

    /**
     * Сервер отдаёт заголовки и первые байты тела, после чего замолкает, не
     * закрывая соединение. Байтов меньше одного буфера чтения: получив их,
     * задача сразу уходит в следующее чтение и блокируется в нём.
     */
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
    fun `отмена обрывает чтение зависшего соединения сразу`() = runBlocking {
        startStallingServer()
        val request = DownloadRequest.Builder(
            "http://127.0.0.1:${serverSocket.localPort}/video.mp4",
            tempFolder.root.absolutePath,
            "video.mp4",
        ).readTimeout(8_000).build()

        val firstBytes = CountDownLatch(1)
        val job = launch(Dispatchers.IO) {
            DownloadTask(request, NoOpsDbHelper()).run(onProgress = { firstBytes.countDown() })
        }
        request.job = job
        assertTrue("загрузка не началась", firstBytes.await(5, TimeUnit.SECONDS))

        // Так же отменяет DownloadDispatchers.cancel: статус, затем job.
        request.status = Status.CANCELLED
        job.cancel()
        val finished = withTimeoutOrNull(3_000) { job.join() }

        assertNotNull("задача не остановилась за 3 с после отмены", finished)
        assertFalse("временный файл обязан быть удалён", File(tempFolder.root, "video.mp4.temp").exists())
    }
}
