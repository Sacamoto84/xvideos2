package com.client.xvideos.common.kdownloader

import com.client.xvideos.common.kdownloader.database.DbHelper
import com.client.xvideos.common.kdownloader.database.DownloadModel
import com.client.xvideos.common.kdownloader.internal.removeStaleDownloads
import com.client.xvideos.common.kdownloader.utils.getTempPath
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Уборка остатков оборванных загрузок.
 *
 * Оборванная загрузка оставляет `.temp` и запись в базе ради докачки. Убирать
 * их было некому: уборка существовала, но не вызывалась, а вызвать её было
 * опасно — запись старой попытки указывает на тот же `.temp`, в который может
 * писать новая.
 */
class KDownloaderStaleCleanupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val now = 10L * DAY_MS

    private class StaleModelsDb(private val stale: List<DownloadModel>) : DbHelper {
        val removed = mutableListOf<Int>()
        override suspend fun find(id: Int): DownloadModel? = null
        override suspend fun insert(model: DownloadModel) = Unit
        override suspend fun update(model: DownloadModel) = Unit
        override suspend fun updateProgress(id: Int, downloadedBytes: Long, lastModifiedAt: Long) = Unit
        override suspend fun remove(id: Int) { removed += id }
        override suspend fun getUnwantedModels(days: Int): List<DownloadModel> = stale
        override suspend fun empty() = Unit
    }

    private fun staleModel(id: Int, fileName: String) = DownloadModel(
        id = id,
        url = "https://example.com/$fileName",
        eTag = "",
        dirPath = tempFolder.root.absolutePath,
        fileName = fileName,
        totalBytes = 100,
        downloadedBytes = 10,
        lastModifiedAt = now - 3 * DAY_MS,
    )

    private fun tempFile(fileName: String, modifiedAt: Long): File =
        File(getTempPath(tempFolder.root.absolutePath, fileName)).apply {
            writeText("partial")
            setLastModified(modifiedAt)
        }

    @Test
    fun `старая запись и её давний остаток удаляются`() = runBlocking {
        val db = StaleModelsDb(listOf(staleModel(1, "old.mp4")))
        val temp = tempFile("old.mp4", modifiedAt = now - 3 * DAY_MS)

        removeStaleDownloads(db, days = 1, nowMs = now)

        assertEquals(listOf(1), db.removed)
        assertFalse(temp.exists())
    }

    @Test
    fun `остаток, в который пишет новая попытка, не трогается`() = runBlocking {
        // Запись прежней попытки старая, но файл по тому же пути свежий: его только
        // что создала загрузка с другим id (адрес с новым токеном).
        val db = StaleModelsDb(listOf(staleModel(1, "retry.mp4")))
        val temp = tempFile("retry.mp4", modifiedAt = now - 1_000L)

        removeStaleDownloads(db, days = 1, nowMs = now)

        assertEquals("запись прежней попытки убирается", listOf(1), db.removed)
        assertTrue("файл идущей загрузки остаётся", temp.exists())
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
