package com.client.xvideos.x.feature.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SavedX_DownloadsProgressTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_downloads_progress")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `повторный запуск загрузки того же ролика отклоняется до её окончания`() {
        val downloads = SavedX_Downloads(scope)

        assertTrue(downloads.markStarted(1L))
        assertFalse(downloads.markStarted(1L))

        downloads.onVideoFinished(1L, failed = false)
        assertTrue(downloads.markStarted(1L))
    }

    @Test
    fun `параллельные загрузки дают общий прогресс, окончание одной не выставляет готово`() {
        val downloads = SavedX_Downloads(scope)
        downloads.markStarted(1L)
        downloads.markStarted(2L)

        downloads.onVideoProgress(1L, 0.2f)
        downloads.onVideoProgress(2L, 0.6f)
        assertEquals(0.4f, downloads.percent.value, 0.0001f)

        downloads.onVideoFinished(1L, failed = false)
        assertTrue(downloads.isDownloading)
        assertEquals(0.6f, downloads.percent.value, 0.0001f)

        downloads.onVideoFinished(2L, failed = true)
        assertTrue(downloads.isError)
    }

    @Test
    fun `поздние колбэки снятой загрузки игнорируются`() {
        val downloads = SavedX_Downloads(scope)
        downloads.markStarted(1L)
        downloads.onVideoFinished(1L, failed = false)

        downloads.onVideoProgress(1L, 0.5f)
        downloads.onVideoFinished(1L, failed = true)

        assertTrue(downloads.isIdle)
    }
}
