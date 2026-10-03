package com.client.xvideos.x.feature.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.x.model.ItemsX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Что считается сохранённым роликом на диске: видео и его `.info` вместе.
 * Видео без `.info` в списке не показывается, значит и «сохранённым» быть не
 * должно — иначе его не видно и нельзя ни удалить, ни скачать заново.
 */
class SavedX_DownloadsStorageTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private lateinit var dir: File

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_downloads_storage")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        dir = File(AppPath.x_cache_download).apply { mkdirs() }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun item(id: Long) = ItemsX(id = id, title = "ролик $id", href = "/video$id/x")

    private fun writeVideo(id: Long) = File(dir, "$id.mp4").writeBytes(ByteArray(16) { 1 })

    private fun writeInfo(id: Long) = File(dir, "$id.info").writeText(AppJson.encodeToString(item(id)))

    /** Список читается в фоне; ждём, пока в нём появится заведомо сохранённый ролик. */
    private fun SavedX_Downloads.awaitSaved(id: Long) {
        val deadline = System.currentTimeMillis() + 3_000
        while (!contains(id) && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertTrue("ролик $id не появился в сохранённых", contains(id))
    }

    @Test
    fun `видео без info не считается сохранённым`() {
        writeVideo(1L)
        writeInfo(1L)
        writeVideo(2L)

        val downloads = SavedX_Downloads(scope)
        downloads.awaitSaved(1L)

        assertFalse("видео без .info нет в списке, но оно числится сохранённым", downloads.contains(2L))
    }

    @Test
    fun `если info не записался, видео удаляется и загрузка считается ошибкой`() {
        val downloads = SavedX_Downloads(scope)
        writeVideo(5L)
        // Каталог на месте файла: запись .info туда невозможна.
        File(dir, "5.info/занято").apply { parentFile.mkdirs() }.writeText("x")
        downloads.markStarted(5L)
        downloads.onVideoProgress(5L, 0.9f)

        downloads.completeDownload(item(5L))

        assertFalse("видео без .info должно быть удалено", File(dir, "5.mp4").exists())
        assertTrue(downloads.isError)
    }

    @Test
    fun `успешная загрузка пишет info и попадает в сохранённые`() {
        val downloads = SavedX_Downloads(scope)
        writeVideo(7L)
        downloads.markStarted(7L)

        downloads.completeDownload(item(7L))

        assertTrue(File(dir, "7.info").exists())
        assertTrue(downloads.contains(7L))
        assertTrue(downloads.isIdle)
    }
}
