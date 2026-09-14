package com.client.xvideos.common.kdownloader

import com.client.xvideos.common.kdownloader.database.NoOpsDbHelper
import com.client.xvideos.common.kdownloader.internal.DownloadDispatchers
import com.client.xvideos.common.kdownloader.internal.DownloadRequest
import com.client.xvideos.common.kdownloader.internal.DownloadRequestQueue
import com.client.xvideos.common.kdownloader.utils.renameFileName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class KDownloaderQueueTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `renameFileName successfully renames source file to destination`() {
        val root = tempFolder.newFolder("rename_test")
        val source = File(root, "video.mp4.temp").apply { writeText("downloaded-content") }
        val target = File(root, "video.mp4")

        renameFileName(source.absolutePath, target.absolutePath)

        assertFalse(source.exists())
        assertTrue(target.exists())
        assertEquals("downloaded-content", target.readText())
    }

    @Test(expected = IOException::class)
    fun `renameFileName throws when source does not exist`() {
        val root = tempFolder.newFolder("rename_nonexistent")
        val source = File(root, "ghost.temp")
        val target = File(root, "video.mp4")

        renameFileName(source.absolutePath, target.absolutePath)
    }

    @Test
    fun `DownloadRequestQueue remove clears request from queue`() {
        val downloader = DownloadDispatchers(NoOpsDbHelper())
        val queue = DownloadRequestQueue(downloader)
        val req = DownloadRequest.Builder("https://example.com/file.mp4", tempFolder.root.absolutePath, "file.mp4")
            .build()

        queue.enqueue(req)
        assertEquals(Status.QUEUED, queue.status(req.downloadId))

        queue.remove(req.downloadId)
        assertEquals(Status.UNKNOWN, queue.status(req.downloadId))
        assertTrue(queue.getAllRequests().isEmpty())
    }

    @Test
    fun `DownloadRequestQueue enqueue returns same downloadId for duplicate queued request`() {
        val downloader = DownloadDispatchers(NoOpsDbHelper())
        val queue = DownloadRequestQueue(downloader)
        val req1 = DownloadRequest.Builder("https://example.com/file.mp4", tempFolder.root.absolutePath, "file.mp4").build()
        val req2 = DownloadRequest.Builder("https://example.com/file.mp4", tempFolder.root.absolutePath, "file.mp4").build()

        val id1 = queue.enqueue(req1)
        val id2 = queue.enqueue(req2)

        assertEquals(id1, id2)
        assertEquals(1, queue.getAllRequests().size)
    }

    @Test
    fun `DownloadRequestQueue cancelAll marks requests cancelled and clears queue`() {
        val downloader = DownloadDispatchers(NoOpsDbHelper())
        val queue = DownloadRequestQueue(downloader)
        val req1 = DownloadRequest.Builder("https://example.com/file1.mp4", tempFolder.root.absolutePath, "file1.mp4").build()
        val req2 = DownloadRequest.Builder("https://example.com/file2.mp4", tempFolder.root.absolutePath, "file2.mp4").build()

        queue.enqueue(req1)
        queue.enqueue(req2)
        assertEquals(2, queue.getAllRequests().size)

        queue.cancelAll()

        assertTrue(queue.getAllRequests().isEmpty())
        assertEquals(Status.UNKNOWN, queue.status(req1.downloadId))
        assertEquals(Status.UNKNOWN, queue.status(req2.downloadId))
        assertEquals(Status.CANCELLED, req1.status)
        assertEquals(Status.CANCELLED, req2.status)
    }

    @Test
    fun `renameFileName succeeds when file stream is closed before renaming`() {
        val root = tempFolder.newFolder("rename_stream_test")
        val source = File(root, "video.mp4.temp")
        val raf = java.io.RandomAccessFile(source, "rw")
        raf.writeBytes("partially-downloaded-stream")
        raf.close()
        val target = File(root, "video.mp4")

        renameFileName(source.absolutePath, target.absolutePath)

        assertFalse(source.exists())
        assertTrue(target.exists())
        assertEquals("partially-downloaded-stream", target.readText())
    }

    @Test
    fun `DownloadModel retains dirPath and fileName for cleanup path computation`() {
        val model = com.client.xvideos.common.kdownloader.database.DownloadModel(
            id = 42,
            url = "https://example.com/video.mp4",
            eTag = "etag123",
            dirPath = "/storage/downloads",
            fileName = "video.mp4",
            totalBytes = 1000L,
            downloadedBytes = 500L,
            lastModifiedAt = 12345L
        )

        val tempPath = com.client.xvideos.common.kdownloader.utils.getTempPath(model.dirPath, model.fileName)
        assertTrue(tempPath.contains("video.mp4.temp"))
        assertTrue(tempPath.contains("/storage/downloads"))
    }
}
