package com.client.xvideos.common.kdownloader

import com.client.xvideos.common.kdownloader.database.DbHelper
import com.client.xvideos.common.kdownloader.database.DownloadModel
import com.client.xvideos.common.kdownloader.database.NoOpsDbHelper
import com.client.xvideos.common.kdownloader.internal.DownloadDispatchers
import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import com.client.xvideos.common.kdownloader.internal.DownloadRequestQueue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * То, что у загрузчика общее: один файл на несколько запросов и одна база на
 * несколько экземпляров.
 */
class KDownloaderSharedStateTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /** Адрес, на котором никто не слушает: фоновая задача падает сразу и в сеть не ходит. */
    private val url = "http://127.0.0.1:1/file.mp4"

    private fun request() = DownloadRequest.Builder(url, tempFolder.root.absolutePath, "file.mp4").build()

    private class RecordingListener(
        private val name: String,
        private val events: MutableList<String>,
    ) : DownloadRequest.Listener {
        override fun onStart() { events += "$name:start" }
        override fun onProgress(value: Int) { events += "$name:progress" }
        override fun onPause() { events += "$name:pause" }
        override fun onCompleted() { events += "$name:completed" }
        override fun onError(error: String) { events += "$name:error:$error" }
    }

    private class InMemoryDbHelper : DbHelper {
        val models = ConcurrentHashMap<Int, DownloadModel>()
        override suspend fun find(id: Int): DownloadModel? = models[id]
        override suspend fun insert(model: DownloadModel) { models[model.id] = model }
        override suspend fun update(model: DownloadModel) { models[model.id] = model }
        override suspend fun updateProgress(id: Int, downloadedBytes: Long, lastModifiedAt: Long) = Unit
        override suspend fun remove(id: Int) { models.remove(id) }
        override suspend fun getUnwantedModels(days: Int): List<DownloadModel>? = null
        override suspend fun empty() { models.clear() }
    }

    @Test
    fun `повторный запрос того же файла получает исход первой загрузки`() {
        val queue = DownloadRequestQueue(DownloadDispatchers(NoOpsDbHelper()))
        val events = Collections.synchronizedList(mutableListOf<String>())
        val first = request().also { it.listener = RecordingListener("first", events) }
        val second = request().also { it.listener = RecordingListener("second", events) }

        queue.enqueue(first)
        queue.enqueue(second)
        first.listener?.onCompleted()

        assertEquals(listOf("first:completed", "second:completed"), events)
    }

    @Test
    fun `cancelAll не стирает записи загрузок другого экземпляра`() {
        val sharedDb = InMemoryDbHelper()
        val foreign = DownloadModel(
            id = 999,
            url = "http://127.0.0.1:1/other.mp4",
            eTag = "",
            dirPath = tempFolder.root.absolutePath,
            fileName = "other.mp4",
            totalBytes = 100L,
            downloadedBytes = 50L,
            lastModifiedAt = 1L,
        )
        runBlocking { sharedDb.insert(foreign) }

        KDownloader.createForTesting(dbHelper = sharedDb).cancelAll()
        // Очистка базы шла в фоне: даём ей время случиться, если она ещё есть.
        Thread.sleep(400)

        assertNotNull("запись чужой загрузки обязана уцелеть", sharedDb.models[999])
    }

    @Test
    fun `отмена приходит в onCancelled, а не в onError`() {
        val downloader = KDownloader.createForTesting()
        val request = downloader.newRequestBuilder(url, tempFolder.root.absolutePath, "file.mp4").build()
        val events = Collections.synchronizedList(mutableListOf<String>())

        downloader.enqueue(
            request,
            onError = { events += "error:$it" },
            onCancelled = { events += "cancelled" },
        )
        request.listener?.onError(Constants.CANCELLED)

        assertEquals(listOf("cancelled"), events)
    }

    @Test
    fun `без onCancelled отмена по-прежнему приходит в onError`() {
        val downloader = KDownloader.createForTesting()
        val request = downloader.newRequestBuilder(url, tempFolder.root.absolutePath, "file.mp4").build()
        val events = Collections.synchronizedList(mutableListOf<String>())

        downloader.enqueue(request, onError = { events += "error:$it" })
        request.listener?.onError(Constants.CANCELLED)

        assertEquals(listOf("error:Cancelled"), events)
    }
}
