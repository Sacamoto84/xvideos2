package com.client.xvideos.l.featured.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.offlineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Перечитывание сохранённого L после восстановления бэкапа: оно меняет файлы
 * мимо хранилищ, а коллекции, открытые за сеанс, лежат в памяти.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedLRefreshAllTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var savedL: SavedL

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_l_refresh")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        Dispatchers.setMain(Dispatchers.Unconfined)
        val db = AppFileDatabase()
        savedL = SavedL(db, scope, Luscious(CoroutineScope(Job()), offlineRepository(db)))
    }

    @After
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    /** Кладёт сохранённый элемент в папку коллекции мимо хранилища — как это делает восстановление. */
    private fun writeItem(collection: String, folder: String) {
        val dir = File(File(AppPath.l_collection, collection), folder).apply { mkdirs() }
        File(dir, "media.jpg").writeText("data")
        writeCollectionMetadata(
            File(dir, L_METADATA_FILE_NAME),
            LSavedLikeMetadata(
                folderName = folder,
                mediaFileName = "media.jpg",
                sourceMediaUrl = "http://host/$folder.jpg",
                sourceOriginalUrl = "http://host/$folder.jpg",
            ),
        )
    }

    private fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(20)
        }
        return condition()
    }

    @Test
    fun `коллекция, открытая до восстановления, после него перечитывается`() {
        writeItem("c1", "a")
        val items = savedL.collection.getCollectionItems("c1")
        assertTrue("коллекция не прочиталась", awaitUntil { items.value?.size == 1 })
        // Пользователь вышел из коллекции и ушёл на страницу бэкапа.
        savedL.collection.exitCollection()

        writeItem("c1", "b")
        savedL.refreshAll()

        assertTrue(
            "в памяти осталось прежнее содержимое: ${items.value?.size} элементов",
            awaitUntil { items.value?.size == 2 },
        )
    }

    @Test
    fun `коллекция, исчезнувшая при восстановлении, после него пуста`() {
        writeItem("c1", "a")
        val items = savedL.collection.getCollectionItems("c1")
        assertTrue("коллекция не прочиталась", awaitUntil { items.value?.size == 1 })
        savedL.collection.exitCollection()

        File(AppPath.l_collection, "c1").deleteRecursively()
        savedL.refreshAll()

        assertTrue("в памяти остались элементы удалённой коллекции", awaitUntil { items.value?.isEmpty() == true })
        assertEquals("чтение не должно создавать папку заново", false, File(AppPath.l_collection, "c1").exists())
    }
}
