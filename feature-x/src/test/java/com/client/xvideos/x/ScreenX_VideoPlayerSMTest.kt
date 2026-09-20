package com.client.xvideos.x

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayerSM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `isFullScreen изначально false`() {
        val sm = ScreenX_VideoPlayerSM("https://example.com/video1", AppFileDatabase())
        assertFalse(sm.isFullScreen)
    }

    @Test
    fun `toggleFullScreen переключает режим полного экрана`() {
        val sm = ScreenX_VideoPlayerSM("https://example.com/video1", AppFileDatabase())
        assertFalse(sm.isFullScreen)

        sm.toggleFullScreen()
        assertTrue(sm.isFullScreen)

        sm.toggleFullScreen()
        assertFalse(sm.isFullScreen)
    }

    @Test
    fun `enterFullScreen и exitFullScreen корректно меняют состояние`() {
        val sm = ScreenX_VideoPlayerSM("https://example.com/video1", AppFileDatabase())
        sm.enterFullScreen()
        assertTrue(sm.isFullScreen)

        sm.exitFullScreen()
        assertFalse(sm.isFullScreen)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `openFullScreen переключает в полный экран на месте`() {
        val sm = ScreenX_VideoPlayerSM("https://example.com/video1", AppFileDatabase())
        sm.openFullScreen()
        assertTrue(sm.isFullScreen)
    }

    @Test
    fun `onPlaybackError сбрасывает полноэкранный режим`() {
        val sm = ScreenX_VideoPlayerSM("https://example.com/video1", AppFileDatabase())
        sm.enterFullScreen()
        assertTrue(sm.isFullScreen)

        sm.onPlaybackError()
        assertFalse(sm.isFullScreen)
        assertTrue(sm.isError)
    }

    @Test
    fun `url нормализуется автоматически при создании SM`() {
        val sm1 = ScreenX_VideoPlayerSM("video123", AppFileDatabase())
        assertEquals("$urlStart/video123", sm1.url)

        val sm2 = ScreenX_VideoPlayerSM("/video123", AppFileDatabase())
        assertEquals("$urlStart/video123", sm2.url)

        val sm3 = ScreenX_VideoPlayerSM("https://example.com/stream", AppFileDatabase())
        assertEquals("https://example.com/stream", sm3.url)
    }
}
