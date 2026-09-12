package com.client.xvideos.common.download.work

import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.WorkInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DownloadWorkRequestTest {

    @Test
    fun `toWorkData корректно пакует все поля запроса`() {
        val request = DownloadWorkRequest(
            id = "vid_123",
            url = "https://example.com/video.mp4",
            destDir = "/storage/emulated/0/Download",
            fileName = "video_123.mp4",
            title = "Test Video Title",
            tag = "custom_tag",
            metaContent = """{"key":"value"}""",
            metaFileName = "video_123.info",
            headers = mapOf("User-Agent" to "CustomUA", "Referer" to "https://example.com"),
            networkType = NetworkType.UNMETERED,
            requiresCharging = true
        )

        val data = request.toWorkData()

        assertEquals("vid_123", data.getString(DownloadWorkRequest.KEY_ID))
        assertEquals("https://example.com/video.mp4", data.getString(DownloadWorkRequest.KEY_URL))
        assertEquals("/storage/emulated/0/Download", data.getString(DownloadWorkRequest.KEY_DEST_DIR))
        assertEquals("video_123.mp4", data.getString(DownloadWorkRequest.KEY_FILE_NAME))
        assertEquals("Test Video Title", data.getString(DownloadWorkRequest.KEY_TITLE))
        assertEquals("custom_tag", data.getString(DownloadWorkRequest.KEY_TAG))
        assertEquals("""{"key":"value"}""", data.getString(DownloadWorkRequest.KEY_META_CONTENT))
        assertEquals("video_123.info", data.getString(DownloadWorkRequest.KEY_META_FILE_NAME))

        val headers = DownloadWorkRequest.parseHeaders(data.getString(DownloadWorkRequest.KEY_HEADERS))
        assertEquals("CustomUA", headers["User-Agent"])
        assertEquals("https://example.com", headers["Referer"])
    }

    @Test
    fun `parseHeaders корректно обрабатывает пустые и некорректные строки`() {
        assertTrue(DownloadWorkRequest.parseHeaders(null).isEmpty())
        assertTrue(DownloadWorkRequest.parseHeaders("").isEmpty())
        assertTrue(DownloadWorkRequest.parseHeaders("   ").isEmpty())
        assertTrue(DownloadWorkRequest.parseHeaders("bad_string_without_equal").isEmpty())

        val headers = DownloadWorkRequest.parseHeaders("Key1=Value1; Key2=Value2 ;Key3=Val=ue3")
        assertEquals(3, headers.size)
        assertEquals("Value1", headers["Key1"])
        assertEquals("Value2", headers["Key2"])
        assertEquals("Val=ue3", headers["Key3"])
    }

    @Test
    fun `parseHeaders и toWorkData сохраняют заголовки с точкой с запятой в значении`() {
        val originalHeaders = mapOf(
            "Cookie" to "session_id=abcdef123456; user_token=xyz789; theme=dark",
            "Accept" to "text/html; charset=utf-8",
            "User-Agent" to "TestAgent"
        )
        val request = DownloadWorkRequest(
            id = "test_cookie",
            url = "https://example.com/video.mp4",
            destDir = "/tmp",
            fileName = "video.mp4",
            title = "Test Cookie",
            headers = originalHeaders
        )

        val workData = request.toWorkData()
        val parsed = DownloadWorkRequest.parseHeaders(workData.getString(DownloadWorkRequest.KEY_HEADERS))

        assertEquals(3, parsed.size)
        assertEquals("session_id=abcdef123456; user_token=xyz789; theme=dark", parsed["Cookie"])
        assertEquals("text/html; charset=utf-8", parsed["Accept"])
        assertEquals("TestAgent", parsed["User-Agent"])
    }

    @Test
    fun `DownloadWorkRequest отвергает опасные имена файлов с выходом за пределы каталога`() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            DownloadWorkRequest(
                id = "bad_1",
                url = "https://example.com/a.mp4",
                destDir = "/tmp",
                fileName = "../malicious.mp4",
                title = "Bad"
            )
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            DownloadWorkRequest(
                id = "bad_2",
                url = "https://example.com/a.mp4",
                destDir = "/tmp",
                fileName = "a/b/c.mp4",
                title = "Bad"
            )
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            DownloadWorkRequest(
                id = "bad_3",
                url = "https://example.com/a.mp4",
                destDir = "/tmp",
                fileName = "good.mp4",
                title = "Bad",
                metaFileName = "../escaped.info"
            )
        }
    }

    @Test
    fun `DownloadWorkState correctly maps from WorkInfo`() {
        val uuid = UUID.randomUUID()
        val progressData = Data.Builder()
            .putInt(DownloadWorkRequest.KEY_PROGRESS, 65)
            .putLong(DownloadWorkRequest.KEY_BYTES_DOWNLOADED, 6500L)
            .putLong(DownloadWorkRequest.KEY_TOTAL_BYTES, 10000L)
            .build()

        val outputData = Data.Builder()
            .putString(DownloadWorkRequest.KEY_OUTPUT_FILE_PATH, "/path/to/file.mp4")
            .build()

        val workInfo = WorkInfo(
            uuid,
            WorkInfo.State.RUNNING,
            setOf(MediaDownloadWorker.WORK_TAG_DOWNLOAD, "my_video_tag"),
            outputData,
            progressData,
            0,
            0,
            androidx.work.Constraints.NONE,
            0L,
            null,
            0L,
            0
        )

        val state = DownloadWorkState.fromWorkInfo(workInfo)

        assertEquals(uuid, state.workId)
        assertEquals("my_video_tag", state.tag)
        assertEquals(DownloadStatus.RUNNING, state.status)
        assertEquals(65, state.progress)
        assertEquals(6500L, state.bytesDownloaded)
        assertEquals(10000L, state.totalBytes)
        assertEquals("/path/to/file.mp4", state.filePath)
        assertNull(state.error)
        assertFalse(state.isFinished)
    }

    @Test
    fun `DownloadWorkState marks SUCCEEDED, FAILED, and CANCELLED as finished`() {
        val uuid = UUID.randomUUID()
        val emptyData = Data.EMPTY

        val succeededInfo = WorkInfo(
            uuid,
            WorkInfo.State.SUCCEEDED,
            setOf("tag"),
            emptyData,
            emptyData,
            0,
            0,
            androidx.work.Constraints.NONE,
            0L,
            null,
            0L,
            0
        )
        assertTrue(DownloadWorkState.fromWorkInfo(succeededInfo).isFinished)

        val failedInfo = WorkInfo(
            uuid,
            WorkInfo.State.FAILED,
            setOf("tag"),
            emptyData,
            emptyData,
            0,
            0,
            androidx.work.Constraints.NONE,
            0L,
            null,
            0L,
            0
        )
        assertTrue(DownloadWorkState.fromWorkInfo(failedInfo).isFinished)

        val cancelledInfo = WorkInfo(
            uuid,
            WorkInfo.State.CANCELLED,
            setOf("tag"),
            emptyData,
            emptyData,
            0,
            0,
            androidx.work.Constraints.NONE,
            0L,
            null,
            0L,
            0
        )
        assertTrue(DownloadWorkState.fromWorkInfo(cancelledInfo).isFinished)
    }
}
