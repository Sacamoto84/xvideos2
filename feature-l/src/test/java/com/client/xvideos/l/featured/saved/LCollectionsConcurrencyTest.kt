package com.client.xvideos.l.featured.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.offlineRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Мутации коллекций L, которые пересекаются во времени: добавление качает
 * файлы и длится долго, а пользователь тем временем делает следующее действие.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LCollectionsConcurrencyTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val server = GateServer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var luscious: Luscious
    private lateinit var collections: SavedL_Collection

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_l_collections")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        Dispatchers.setMain(Dispatchers.Unconfined)
        luscious = Luscious(CoroutineScope(Job()), offlineRepository(AppFileDatabase()))
        collections = SavedL_Collection(scope, luscious)
    }

    @After
    fun tearDown() {
        server.stop()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun pic(name: String) = PicsDetails(url_to_original = server.url(name), album = null, id = name)

    private fun collectionDir(name: String) = File(AppPath.l_collection, name)

    /** Имена папок сохранённых элементов коллекции: сохранён тот, у кого записаны метаданные. */
    private fun savedItems(collection: String): List<String> =
        collectionDir(collection).listFiles()
            ?.filter { File(it, L_METADATA_FILE_NAME).exists() }
            ?.map { it.name }
            .orEmpty()

    private fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(20)
        }
        return condition()
    }

    @Test
    fun `второе добавление не отменяет первое`() {
        collections.add(pic("slow.jpg"), "c1")
        server.awaitSlowRequested()

        collections.add(pic("fast.jpg"), "c1")
        Thread.sleep(200)
        server.releaseSlow()

        assertTrue(
            "в коллекции должны оказаться оба элемента, есть: ${savedItems("c1")}",
            awaitUntil { savedItems("c1").size == 2 },
        )
    }

    @Test
    fun `удаление коллекции во время добавления не возвращает её обратно`() {
        collections.addAll(listOf(pic("slow.jpg"), pic("fast.jpg")), "c1")
        server.awaitSlowRequested()

        collections.deleteCollection("c1")
        Thread.sleep(200)
        server.releaseSlow()
        Thread.sleep(1_500)

        assertFalse("удалённая коллекция появилась снова: ${savedItems("c1")}", collectionDir("c1").exists())
    }

    @Test
    fun `переименование во время добавления забирает все элементы под новое имя`() {
        collections.addAll(listOf(pic("slow.jpg"), pic("fast.jpg")), "old")
        server.awaitSlowRequested()

        collections.renameCollection("old", "new")
        Thread.sleep(200)
        server.releaseSlow()

        assertTrue(
            "под новым именем должны оказаться оба элемента, есть: ${savedItems("new")}",
            awaitUntil { savedItems("new").size == 2 },
        )
        assertFalse("коллекция со старым именем появилась снова", collectionDir("old").exists())
    }

    @Test
    fun `отмена сохранения пробрасывается, а не возвращается как ошибка загрузки`() {
        val progressScope = CoroutineScope(Job())
        val root = tmp.newFolder("persist_root")
        var returned: Result<File>? = null
        var thrown: Throwable? = null

        runBlocking {
            launch(Dispatchers.IO) {
                // Корутина отменена: первая же приостановка внутри сохранения бросит отмену.
                coroutineContext.job.cancel()
                try {
                    returned = lPersistPicsDetailsToFolder(
                        item = pic("fast.jpg"),
                        root = root,
                        luscious = luscious,
                        progress = LDownloadProgress(progressScope),
                    )
                } catch (e: Throwable) {
                    thrown = e
                }
            }.join()
        }
        progressScope.cancel()

        assertNull("отмена вернулась как Result: $returned", returned)
        assertTrue("ожидалась отмена, пришло $thrown", thrown is CancellationException)
    }

    @Test
    fun `чтение несуществующей коллекции не создаёт её папку`() {
        val missing = File(tmp.newFolder("collections_root"), "нет такой")

        val items = lReadCollectionItems(missing)

        assertEquals(emptyList<PicsDetails>(), items)
        assertFalse("чтение создало папку коллекции", missing.exists())
    }
}
