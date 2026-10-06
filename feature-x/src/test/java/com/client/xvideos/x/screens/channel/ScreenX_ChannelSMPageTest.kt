package com.client.xvideos.x.screens.channel

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ChannelSortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Номер страницы пейджера канала. Планировщик главного потока не запускается:
 * загрузки ScreenModel стоят в очереди и в сеть не идут.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenX_ChannelSMPageTest {

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @BeforeClass
        @JvmStatic
        fun setUp() {
            Dispatchers.setMain(testDispatcher)
            val tempDir = Files.createTempDirectory("app_path_test_channel").toFile()
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
    fun `предел страниц растёт вместе с текущей страницей`() {
        val sm = ScreenX_ChannelSM(SavedX(CoroutineScope(Dispatchers.Unconfined)), slug = "sample")
        assertEquals(1, sm.uiState.maxPages)

        sm.currentPage = 9

        // Число видео неизвестно: предел считается от текущей страницы, а не застывает.
        assertEquals(9, sm.uiState.currentPage)
        assertEquals(10, sm.uiState.maxPages)
        assertEquals(9, sm.currentPage)
    }

    /** Пейджер не запрашивает страницу 0, пока её грузит первая загрузка. */
    @Test
    fun `первая загрузка отмечает страницу 0 загружаемой`() {
        val sm = ScreenX_ChannelSM(SavedX(CoroutineScope(Dispatchers.Unconfined)), slug = "sample")
        assertTrue(0 in sm.loadingPages)

        sm.changeSort(ChannelSortOrder.NEW)
        assertTrue(0 in sm.loadingPages)
    }

    @Test
    fun `страница пейджера, выставленная экраном, видна в uiState`() {
        val sm = ScreenX_ChannelSM(SavedX(CoroutineScope(Dispatchers.Unconfined)), slug = "sample")

        sm.currentPage = 3

        assertEquals(3, sm.uiState.currentPage)
    }
}
