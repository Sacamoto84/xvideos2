package com.client.xvideos.r.common.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.r.model.GifsInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
 * Правка одной коллекции R перечитывает с диска её одну. Раньше добавление и
 * удаление элемента, создание и удаление коллекции заканчивались чтением всех
 * коллекций со всеми элементами.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RSavedCollectionRefreshTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var collections: R_Saved_Collection

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_r_collections")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        collections = R_Saved_Collection(scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun gif(id: String) = GifsInfo(id = id, userName = "author")

    private fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(20)
        }
        return condition()
    }

    private fun itemsOf(name: String): List<String>? =
        collections.collectionList.firstOrNull { it.collection == name }?.items?.map { it.id }

    /**
     * Две коллекции с одним элементом в каждой, обе уже в списке. Правки идут
     * по одной: в тесте нет главного потока, который в приложении выстраивает
     * их публикацию в очередь.
     */
    private fun twoCollections() {
        collections.addCollection(gif("a1"), "A")
        assertTrue("коллекция A не появилась", awaitUntil { itemsOf("A") == listOf("a1") })
        collections.addCollection(gif("b1"), "B")
        assertTrue("коллекция B не появилась", awaitUntil { itemsOf("B") == listOf("b1") })
    }

    @Test
    fun `добавление в коллекцию не перечитывает остальные`() {
        twoCollections()
        // Соседняя коллекция изменилась на диске мимо хранилища: полное чтение её увидит.
        collections.collectionDb.insert("b2", "B", gif("b2"))

        collections.addCollection(gif("a2"), "A")

        assertTrue("элемент не появился в коллекции", awaitUntil { itemsOf("A")?.toSet() == setOf("a1", "a2") })
        Thread.sleep(200)
        assertEquals("после правки одной коллекции перечитаны все", listOf("b1"), itemsOf("B"))
    }

    @Test
    fun `удаление элемента убирает его из своей коллекции`() {
        twoCollections()

        collections.deleteItemFromCollection("a1", "A")

        assertTrue(awaitUntil { itemsOf("A") == emptyList<String>() })
        assertEquals(listOf("b1"), itemsOf("B"))
    }

    @Test
    fun `новая коллекция встаёт в список по имени, удалённая из него уходит`() {
        twoCollections()

        collections.createCollection("Aa")
        assertTrue(awaitUntil { collections.collectionList.map { it.collection } == listOf("A", "Aa", "B") })

        collections.deleteCollection("A")
        assertTrue(awaitUntil { collections.collectionList.map { it.collection } == listOf("Aa", "B") })
    }
}
