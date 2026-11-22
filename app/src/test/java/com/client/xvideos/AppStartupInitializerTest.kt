package com.client.xvideos

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.storage.StorageCleanupGate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupInitializerTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun `menu waits for storage recovery without opening the database`() = runTest {
        val rollback = CompletableDeferred<Unit>()
        val gate = StorageCleanupGate().apply { start(this@runTest) { rollback.await() } }
        val initializer = AppStartupInitializer(
            storageCleanupGate = gate,
            db = { error("The startup gate must not open the database") },
            context = ContextWrapper(null),
            scope = this,
        )

        val waiting = launch { initializer.awaitStorage() }
        runCurrent()
        assertFalse(waiting.isCompleted)

        rollback.complete(Unit)
        runCurrent()
        assertTrue(waiting.isCompleted)
    }

    @Test
    fun `background cleanup keeps new transfers`() = runTest {
        val files = folder.newFolder("files")
        val cache = folder.newFolder("cache")
        AppPath.init(object : ContextWrapper(null) {
            override fun getFilesDir() = files
            override fun getCacheDir() = cache
        })
        val inbox = File(AppPath.p2p_inbox)
        val old = File(inbox, "old.zip").apply { writeText("previous transfer") }

        AppPath.prepareTransientDirs()
        assertFalse(old.exists())
        val active = File(inbox, "active.zip").apply { writeText("current transfer") }

        AppPath.deleteTransientTrash()
        assertTrue(active.isFile)
        assertTrue(inbox.parentFile!!.listFiles().orEmpty().none { it.name.startsWith(".startup-trash-") })
    }
}
