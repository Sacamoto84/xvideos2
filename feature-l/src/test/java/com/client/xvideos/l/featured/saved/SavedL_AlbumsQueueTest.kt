package com.client.xvideos.l.featured.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.model.AlbumDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Сохранённые альбомы L: действия подряд. Раньше каждое новое отменяло
 * предыдущее — файл уже записан, а обновление списка стояло за переходом на
 * главный поток и отменялось: альбом на диске есть, значок «сохранён» не горит.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedL_AlbumsQueueTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Главный поток стоит, пока тест его не прокрутит: так запись файла успевает раньше обновления списка. */
    private val main = StandardTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var db: AppFileDatabase
    private lateinit var albums: SavedL_Albums

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_l_albums")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        Dispatchers.setMain(main)
        db = AppFileDatabase()
        albums = SavedL_Albums(db, scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun album(id: String) = AlbumDetails(id = id, title = "Album $id")

    private fun albumFile(id: String) = File(AppPath.l_albums, "$id.album")

    /** Ждёт условия, прокручивая главный поток: обновление списка идёт через него. */
    private fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            main.scheduler.advanceUntilIdle()
            if (condition()) return true
            Thread.sleep(20)
        }
        main.scheduler.advanceUntilIdle()
        return condition()
    }

    @Test
    fun `сохранение второго альбома не теряет первый в списке`() {
        albums.add(album("101"))
        // Файл первого записан, а до списка дело ещё не дошло: главный поток стоит.
        assertTrue("файл первого альбома не записан", awaitFile("101"))

        albums.add(album("102"))

        val bothListed = awaitUntil { albums.list.map { it.id }.sorted() == listOf("101", "102") }
        assertTrue("в списке должны быть оба альбома, есть: ${albums.list.map { it.id }}", bothListed)
        assertTrue(albumFile("101").exists() && albumFile("102").exists())
    }

    @Test
    fun `удаление во время сохранения другого альбома выполняются оба`() {
        albums.add(album("201"))
        assertTrue(awaitUntil { albums.list.any { it.id == "201" } })

        albums.add(album("202"))
        assertTrue("файл второго альбома не записан", awaitFile("202"))
        albums.remove(album("201"))

        val settled = awaitUntil { albums.list.map { it.id } == listOf("202") }
        assertTrue("должен остаться только второй альбом, есть: ${albums.list.map { it.id }}", settled)
        assertTrue(albumFile("202").exists())
        assertTrue(!albumFile("201").exists())
    }

    @Test
    fun `удаление альбома убирает и оставшийся кэш его картинок`() {
        runBlocking { db.lAlbumPictureCache.put("301", "[]") }
        albums.add(album("301"))
        assertTrue(awaitUntil { albums.list.any { it.id == "301" } })

        albums.remove(album("301"))

        assertTrue(awaitUntil { albums.list.isEmpty() })
        assertTrue(
            "кэш картинок удалённого альбома остался на диске",
            awaitUntil { runBlocking { db.lAlbumPictureCache.get("301") } == null },
        )
        assertEquals(0, albums.count)
        assertNull(albums.findByIdOrNull("301"))
    }

    /** Ждёт файл альбома, не прокручивая главный поток. */
    private fun awaitFile(id: String): Boolean {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (albumFile(id).exists()) return true
            Thread.sleep(10)
        }
        return albumFile(id).exists()
    }
}
