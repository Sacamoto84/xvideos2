package com.client.xvideos.x.feature.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.x.model.ItemsX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SavedX_HistoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val tempDir = tmp.newFolder("app_store_history")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ролики короче 2 минут не сохраняются в историю`() = runTest(testDispatcher) {
        val history = SavedX_History(testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val shortVideo = ItemsX(id = 100L, title = "Short Video", duration = "1 мин.")
        history.updateProgress(shortVideo, positionMs = 30_000L, totalDurationMs = 119_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(history.get(100L))
        assertEquals(0, history.list.size)
    }

    @Test
    fun `случайные открытия менее 5 секунд не создают новую запись`() = runTest(testDispatcher) {
        val history = SavedX_History(testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val video = ItemsX(id = 200L, title = "Long Video", duration = "10 мин.")
        history.updateProgress(video, positionMs = 3_000L, totalDurationMs = 600_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(history.get(200L))
        assertEquals(0, history.list.size)
    }

    @Test
    fun `валидный просмотр сохраняет позицию и доступен через get`() = runTest(testDispatcher) {
        val history = SavedX_History(testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val video = ItemsX(id = 300L, title = "Watched Video", duration = "5 мин.")
        history.updateProgress(video, positionMs = 120_000L, totalDurationMs = 300_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        val entry = history.get(300L)
        assertNotNull(entry)
        assertEquals(120_000L, entry?.lastPositionMs)
        assertEquals(300_000L, entry?.totalDurationMs)
        assertEquals("Watched Video", entry?.item?.title)
    }

    @Test
    fun `досмотр до 95 процентов сбрасывает сохраненную позицию на 0L`() = runTest(testDispatcher) {
        val history = SavedX_History(testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val video = ItemsX(id = 400L, title = "Finished Video", duration = "4 мин.")
        // 230 / 240 = 95.8% (больше порога 95%)
        history.updateProgress(video, positionMs = 230_000L, totalDurationMs = 240_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        val entry = history.get(400L)
        assertNotNull(entry)
        assertEquals(0L, entry?.lastPositionMs) // Сброшено на начало
        assertEquals(240_000L, entry?.totalDurationMs)
    }

    @Test
    fun `delete удаляет запись из памяти и хранилища`() = runTest(testDispatcher) {
        val history = SavedX_History(testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val video = ItemsX(id = 500L, title = "To Delete", duration = "5 мин.")
        history.updateProgress(video, positionMs = 60_000L, totalDurationMs = 300_000L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(history.get(500L))

        history.delete(video)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(history.get(500L))
        assertEquals(0, history.list.size)
    }

    @Test
    fun `clearAll полностью очищает все записи`() = runTest(testDispatcher) {
        val history = SavedX_History(testScope, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()

        val video1 = ItemsX(id = 601L, title = "Video 1")
        val video2 = ItemsX(id = 602L, title = "Video 2")
        history.updateProgress(video1, positionMs = 50_000L, totalDurationMs = 300_000L)
        history.updateProgress(video2, positionMs = 60_000L, totalDurationMs = 300_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, history.list.size)

        history.clearAll()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, history.list.size)
        assertNull(history.get(601L))
        assertNull(history.get(602L))
    }
}
