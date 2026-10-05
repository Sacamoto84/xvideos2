package com.client.xvideos.l.net

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.net.graphQl.GraphQlRequest
import com.client.xvideos.l.repository.HTML_INSTEAD_OF_JSON_PREFIX
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Повтор сбойных страниц альбома: альбом считается загруженным целиком, только
 * когда получена каждая его страница. Раньше повтор после сбоя первой страницы
 * загружал её одну и объявлял альбом полным, а повтор во время идущей загрузки
 * делал то же с половиной альбома — и усечённый набор уходил в кэш на 7 дней.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumPicsDetailsRetryTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_l_album_retry").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }

        private const val ALBUM_ID = 777
        private const val PICS_PER_PAGE = 2

        private fun pageJson(page: Int, totalPages: Int): String {
            val items = (1..PICS_PER_PAGE).joinToString(",") { n ->
                """{"id":"${page * 100 + n}","url_to_original":"https://example.com/$page-$n.jpg"}"""
            }
            val info = """{"total_pages":$totalPages,"total_items":${totalPages * PICS_PER_PAGE},"items_per_page":$PICS_PER_PAGE}"""
            return """{"data":{"picture":{"list":{"info":$info,"items":[$items]}}}}"""
        }
    }

    /** Репозиторий, который отвечает на запросы страниц альбома по сценарию теста. */
    private class PagedRepository(private val totalPages: Int) : Repository(AppFileDatabase()) {
        /** Номера запрошенных страниц по порядку. */
        val requests = mutableListOf<Int>()

        /** Ответ на запрос страницы; `attempt` — который по счёту запрос этой страницы. */
        var respond: suspend (page: Int, attempt: Int) -> Result<String> = { page, _ -> ok(page) }

        override suspend fun openURI(data: String, config: RepositoryUriConfig): Result<String> {
            val page = (1..totalPages).first { GraphQlRequest.pictureListInsideAlbum(ALBUM_ID, it) == data }
            requests += page
            return respond(page, requests.count { it == page })
        }

        fun ok(page: Int): Result<String> = Result.success(pageJson(page, totalPages))
    }

    private val dispatcher = StandardTestDispatcher()

    private fun details(repository: Repository) = AlbumPicsDetails(ALBUM_ID, repository, dispatcher = dispatcher)

    private fun AlbumPicsDetails.picIds(): List<String?> = pics.map { it.id }

    @Test
    fun `повтор после сбоя первой страницы загружает весь альбом`() = runTest(dispatcher) {
        val repository = PagedRepository(totalPages = 3)
        repository.respond = { page, attempt ->
            if (page == 1 && attempt == 1) Result.failure(IOException("нет сети")) else repository.ok(page)
        }
        val details = details(repository)

        details.contentUrls()
        assertEquals(listOf(1), details.failedPages.map { it.page })
        assertNull("после сбоя первой страницы кэшировать нечего", details.bundleSnapshotOrNull())

        details.retryFailedPages()

        assertEquals(listOf(1, 1, 2, 3), repository.requests)
        assertEquals(listOf("101", "102", "201", "202", "301", "302"), details.picIds())
        assertEquals(6, details.bundleSnapshotOrNull()?.pics?.size)
        assertEquals(1f, details.percentLoad)
    }

    @Test
    fun `повтор запрашивает только сбойную страницу и ставит её на место`() = runTest(dispatcher) {
        val repository = PagedRepository(totalPages = 4)
        repository.respond = { page, attempt ->
            if (page == 3 && attempt == 1) Result.failure(IOException("обрыв")) else repository.ok(page)
        }
        val details = details(repository)

        details.contentUrls()
        assertNull("альбом без одной страницы не полный", details.bundleSnapshotOrNull())
        assertTrue("полоса загрузки остаётся, пока есть сбойные страницы", details.percentLoad < 1f)

        details.retryFailedPages()

        assertEquals(listOf(1, 2, 3, 4, 3), repository.requests)
        assertEquals(listOf("101", "102", "201", "202", "301", "302", "401", "402"), details.picIds())
        assertEquals(8, details.bundleSnapshotOrNull()?.pics?.size)
        assertTrue(details.failedPages.isEmpty())
    }

    @Test
    fun `повтор во время идущей загрузки ждёт её конца и не объявляет альбом полным`() = runTest(dispatcher) {
        val thirdPage = CompletableDeferred<Unit>()
        val repository = PagedRepository(totalPages = 4)
        repository.respond = { page, attempt ->
            when {
                page == 2 && attempt == 1 -> Result.failure(IOException("обрыв"))
                page == 3 -> {
                    thirdPage.await()
                    repository.ok(page)
                }
                else -> repository.ok(page)
            }
        }
        val details = details(repository)

        // Загрузка дошла до третьей страницы и ждёт ответа; вторая уже сбойная.
        launch { details.contentUrls() }
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), repository.requests)

        launch { details.retryFailedPages() }
        advanceUntilIdle()

        assertTrue("кнопка повтора занята, пока повтор ждёт", details.isRetryingFailedPages)
        assertEquals("повтор не лезет в идущую загрузку", listOf(1, 2, 3), repository.requests)
        assertNull("пока загрузка идёт, альбом не полный", details.bundleSnapshotOrNull())

        thirdPage.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 3, 4, 2), repository.requests)
        assertEquals(listOf("101", "102", "201", "202", "301", "302", "401", "402"), details.picIds())
        assertEquals(8, details.bundleSnapshotOrNull()?.pics?.size)
        assertFalse(details.isRetryingFailedPages)
    }

    @Test
    fun `новая загрузка отменяет повтор, который ждал прежнюю`() = runTest(dispatcher) {
        val thirdPage = CompletableDeferred<Unit>()
        val repository = PagedRepository(totalPages = 4)
        repository.respond = { page, attempt ->
            when {
                page == 2 && attempt == 1 -> Result.failure(IOException("обрыв"))
                page == 3 && attempt == 1 -> {
                    thirdPage.await()
                    repository.ok(page)
                }
                else -> repository.ok(page)
            }
        }
        val details = details(repository)

        val firstLoad = launch { details.contentUrls() }
        advanceUntilIdle()
        launch { details.retryFailedPages() }
        advanceUntilIdle()

        // «Обновить»: прежняя загрузка отменена, начата новая.
        firstLoad.cancel()
        launch { details.contentUrls() }
        advanceUntilIdle()

        assertEquals(
            "повтор прежней загрузки не должен слать запросы перед новой",
            listOf(1, 2, 3, 1, 2, 3, 4),
            repository.requests,
        )
        assertEquals(8, details.pics.size)
        assertFalse(details.isRetryingFailedPages)
    }

    @Test
    fun `сбой страницы хранит текст для пользователя, а не разметку защиты`() = runTest(dispatcher) {
        val repository = PagedRepository(totalPages = 2)
        repository.respond = { page, _ ->
            if (page == 2) {
                Result.failure(IllegalStateException("$HTML_INSTEAD_OF_JSON_PREFIX: <html><body>Just a moment</body></html>"))
            } else {
                repository.ok(page)
            }
        }
        val details = details(repository)

        details.contentUrls()

        val issue = details.failedPages.single()
        assertEquals(2, issue.page)
        assertTrue(issue.htmlChallenge)
        assertFalse(issue.message, issue.message.contains("<html"))
        assertFalse(issue.message, issue.message.startsWith(HTML_INSTEAD_OF_JSON_PREFIX))
    }
}
