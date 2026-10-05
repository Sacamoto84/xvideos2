package com.client.xvideos.l.featured.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.model.PicsDetails
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
 * Лайки L, которые пересекаются во времени: сохранение качает файлы и длится
 * долго, а пользователь тем временем ставит следующий лайк. Раньше каждое новое
 * действие отменяло предыдущее, и из трёх лайков подряд сохранялся последний.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LLikesConcurrencyTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val server = GateServer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var likes: SavedL_Likes

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_l_likes")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        Dispatchers.setMain(Dispatchers.Unconfined)
        val luscious = Luscious(CoroutineScope(Job()), offlineRepository(AppFileDatabase()))
        likes = SavedL_Likes(luscious, scope)
    }

    @After
    fun tearDown() {
        server.stop()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun pic(name: String) = PicsDetails(url_to_original = server.url(name), album = null, id = name)

    /** Папки сохранённых лайков: сохранён тот, у кого записаны метаданные. */
    private fun savedLikes(): List<File> =
        File(AppPath.l_likes).listFiles()
            ?.filter { File(it, L_METADATA_FILE_NAME).exists() }
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
    fun `второй лайк не отменяет сохранение первого`() {
        likes.add(pic("slow.jpg"))
        server.awaitSlowRequested()

        likes.add(pic("fast.jpg"))
        Thread.sleep(200)
        server.releaseSlow()

        val bothSaved = awaitUntil { savedLikes().size == 2 }
        assertTrue("должны сохраниться оба лайка, есть: ${savedLikes().map { it.name }}", bothSaved)
        val bothListed = awaitUntil { likes.listUrl.size == 2 }
        assertTrue("оба лайка должны появиться в списке, есть: ${likes.listUrl.size}", bothListed)
    }

    @Test
    fun `удаление лайка не обрывает сохранение другого`() {
        likes.add(pic("fast.jpg"))
        assertTrue("первый лайк не появился в списке", awaitUntil { likes.listUrl.size == 1 })
        val savedFile = likes.listUrl.single().url_to_original.orEmpty()

        likes.add(pic("slow.jpg"))
        server.awaitSlowRequested()
        likes.remove(savedFile)
        Thread.sleep(200)
        server.releaseSlow()

        val settled = awaitUntil { savedLikes().singleOrNull()?.name?.contains("slow") == true && likes.listUrl.size == 1 }
        assertTrue(
            "должен остаться только лайк, который сохранялся во время удаления; " +
                "на диске: ${savedLikes().map { it.name }}, в списке: ${likes.listUrl.size}",
            settled,
        )
    }

    @Test
    fun `новый лайк встаёт в начало списка без перечитывания каталога`() {
        likes.add(pic("first.jpg"))
        assertTrue(awaitUntil { likes.listUrl.size == 1 })
        // Список разошёлся с диском: перечитывание каталога вернуло бы первый лайк.
        likes.listUrl.clear()

        likes.add(pic("second.jpg"))

        assertTrue(awaitUntil { likes.listUrl.isNotEmpty() })
        Thread.sleep(300)
        assertEquals("после лайка каталог перечитан целиком", 1, likes.listUrl.size)
        assertEquals(2, savedLikes().size)
    }

    @Test
    fun `повторный лайк той же картинки не удваивает её в списке`() {
        likes.add(pic("same.jpg"))
        assertTrue(awaitUntil { likes.listUrl.size == 1 })

        likes.add(pic("same.jpg"))
        Thread.sleep(500)

        assertEquals(1, likes.listUrl.size)
        assertEquals(1, savedLikes().size)
    }
}
