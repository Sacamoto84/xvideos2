package com.client.xvideos.x

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayerSM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenX_VideoPlayerSMTest {

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @BeforeClass
        @JvmStatic
        fun setUp() {
            Dispatchers.setMain(testDispatcher)
            val tempDir = Files.createTempDirectory("app_path_test_x").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            Dispatchers.resetMain()
        }
    }

    private fun newSm(url: String) = ScreenX_VideoPlayerSM(
        url = url,
        initialItem = null,
        db = AppFileDatabase(),
        saved = SavedX(CoroutineScope(Dispatchers.Unconfined)),
    )

    /** Планировщик главного потока не запускается: сама загрузка стоит в очереди и в сеть не идёт. */
    @Test
    fun `новая загрузка сбрасывает позицию старта, чтобы она перечиталась из истории`() {
        val sm = newSm("/video55555")
        sm.restartFromBeginning()
        assertEquals(0f, sm.resumePositionSeconds)

        sm.loadVideo(forceReload = true)

        assertNull(sm.resumePositionSeconds)
    }

    @Test
    fun `isFullScreen изначально false`() {
        val sm = newSm("https://example.com/video1")
        assertFalse(sm.isFullScreen)
    }

    @Test
    fun `toggleFullScreen переключает режим полного экрана`() {
        val sm = newSm("https://example.com/video1")
        assertFalse(sm.isFullScreen)

        sm.toggleFullScreen()
        assertTrue(sm.isFullScreen)

        sm.toggleFullScreen()
        assertFalse(sm.isFullScreen)
    }

    @Test
    fun `exitFullScreen выходит из полноэкранного режима`() {
        val sm = newSm("https://example.com/video1")
        sm.toggleFullScreen()
        assertTrue(sm.isFullScreen)

        sm.exitFullScreen()
        assertFalse(sm.isFullScreen)
    }

    @Test
    fun `onPlaybackError сбрасывает полноэкранный режим`() {
        val sm = newSm("https://example.com/video1")
        sm.toggleFullScreen()
        assertTrue(sm.isFullScreen)

        sm.onPlaybackError()
        assertFalse(sm.isFullScreen)
        assertTrue(sm.isError)
    }

    @Test
    fun `url нормализуется автоматически при создании SM`() {
        val sm1 = newSm("video123")
        assertEquals("$urlStart/video123", sm1.url)

        val sm2 = newSm("/video123")
        assertEquals("$urlStart/video123", sm2.url)

        val sm3 = newSm("https://example.com/stream")
        assertEquals("https://example.com/stream", sm3.url)
    }

    @Test
    fun `saveProgress устойчив к нечисловым и отрицательным значениям времени`() {
        val sm = newSm("/video99999")
        // Проверяем, что вызов с NaN, Infinity, отрицательными секундами не приводит к крашу
        sm.saveProgress(Float.NaN, -10)
        sm.saveProgress(Float.POSITIVE_INFINITY, 300)
        sm.saveProgress(-5f, 300)
        sm.saveProgress(50f, 300)

        val savedItem = runBlocking { sm.saved.history.get(99999L) }
        // 50s * 1000 = 50000ms
        assertEquals(50_000L, savedItem?.lastPositionMs)
        assertEquals(300_000L, savedItem?.totalDurationMs)
    }

    @Test
    fun `dismissResumeNotice сбрасывает текст уведомления`() {
        val sm = newSm("/video123")
        sm.dismissResumeNotice()
        assertEquals(null, sm.resumeNoticeText)
    }
}
