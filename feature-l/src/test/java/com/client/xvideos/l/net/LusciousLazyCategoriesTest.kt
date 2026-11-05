package com.client.xvideos.l.net

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

/**
 * Проверяет, что создание [Luscious] не производит немедленных сетевых запросов
 * справочника категорий, и что справочник загружается лениво через [Luscious.ensureMediaCategories].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LusciousLazyCategoriesTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_l_lazy_cat").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Unconfined)

    @Before
    fun setUpMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mediaCategoriesFlow.value = null
    }

    @After
    fun tearDownMain() {
        runBlocking {
            scope.coroutineContext.job.cancelAndJoin()
        }
        Dispatchers.resetMain()
        mediaCategoriesFlow.value = null
    }

    private class RecordingRepository : Repository(AppFileDatabase()) {
        val requestCount = AtomicInteger(0)

        override suspend fun openURI(data: String, config: RepositoryUriConfig): Result<String> {
            requestCount.incrementAndGet()
            return Result.success(
                """{"data":{"media_categories":{"genres":[],"filter_settings":null,"languages":[],"content_types":[],"audiences":[]}}}"""
            )
        }
    }

    private suspend fun awaitUntil(condition: () -> Boolean) {
        withTimeout(3_000) {
            while (!condition()) delay(10)
        }
    }

    @Test
    fun `создание Luscious не делает сетевых запросов категорий`() = runBlocking {
        val repo = RecordingRepository()

        Luscious(scope, repo)
        delay(100)

        assertEquals("При создании Luscious сетевые запросы не должны отправляться", 0, repo.requestCount.get())
        assertNull("mediaCategoriesFlow должен быть null до явного вызова", mediaCategoriesFlow.value)
    }

    @Test
    fun `ensureMediaCategories загружает категории лениво и однократно`() = runBlocking {
        val repo = RecordingRepository()

        val luscious = Luscious(scope, repo)
        assertEquals(0, repo.requestCount.get())

        luscious.ensureMediaCategories()
        awaitUntil { mediaCategoriesFlow.value != null }

        assertEquals(1, repo.requestCount.get())
        assertNotNull(mediaCategoriesFlow.value)

        // Повторный вызов не должен делать новый запрос
        luscious.ensureMediaCategories()
        delay(50)
        assertEquals(1, repo.requestCount.get())
    }
}
