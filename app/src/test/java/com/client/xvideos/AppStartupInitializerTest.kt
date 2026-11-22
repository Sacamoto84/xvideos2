package com.client.xvideos

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.storage.StorageCleanupGate
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupInitializerTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            val tempDir = Files.createTempDirectory("app_path_test").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init выполняется только один раз`() = runTest {
        val counter = AtomicInteger(0)
        val initializer = AppStartupInitializer(
            db = {
                counter.incrementAndGet()
                AppFileDatabase()
            },
            blockRed = { BlockRed(this) },
            savedRed = { SavedRed(RedApi(AppFileDatabase()), this) },
            storageCleanupGate = StorageCleanupGate(),
        )

        initializer.init()
        assertEquals(1, counter.get())

        // Повторный вызов не должен перезапускать инициализацию
        initializer.init()
        assertEquals(1, counter.get())
    }

    @Test
    fun `хранилища открываются только после отката и только один раз`() = runTest {
        val rollback = CompletableDeferred<Unit>()
        val gate = StorageCleanupGate().apply { start(this@runTest) { rollback.await() } }
        val reads = AtomicInteger()
        val initializer = AppStartupInitializer(
            db = { AppFileDatabase() },
            blockRed = { BlockRed(this) },
            savedRed = {
                reads.incrementAndGet()
                SavedRed(RedApi(AppFileDatabase()), this)
            },
            storageCleanupGate = gate,
        )
        val first = launch { initializer.init() }
        val second = launch { initializer.init() }
        runCurrent()
        assertEquals(0, reads.get())

        rollback.complete(Unit)
        first.join()
        second.join()
        assertEquals(1, reads.get())
    }
}
