package com.client.xvideos.l.repository

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.net.graphQl.FilterSettings
import com.client.xvideos.l.net.graphQl.GraphQlRequest
import com.client.xvideos.l.net.graphQl.MediaCategories
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Разбор ответов сервера в репозитории серверного избранного L: `null` там, где
 * ждали объект или список, не должен ни ронять приложение, ни выдавать успех за
 * ошибку, а id пользователя сессии не должен переживать смену аккаунта.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LServerFavoritesRepositoryImplTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpPaths() {
            val tempDir = Files.createTempDirectory("app_path_test_l_server_favorites").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }

        private const val ALBUM_ID = 4242
        private val OPERATION = Regex("\"operationName\":\"([A-Za-z0-9_]+)\"")

        private fun bootstrapJson(userId: Long) =
            """{"data":{"media_categories":{"filter_settings":{"user_id":$userId}}}}"""

        private fun albumPageJson(totalPages: Int, items: String) =
            """{"data":{"picture":{"list":{"info":{"total_pages":$totalPages},"items":[$items]}}}}"""

        private fun picture(id: Int, fileName: String, extra: String = "") =
            """{"id":"$id","url_to_original":"https://example.com/$fileName"$extra}"""
    }

    /** Базовый репозиторий, который отвечает на запросы по сценарию теста. */
    private class ScriptedRepository : Repository(AppFileDatabase()) {
        var revision = 0
        var respond: (data: String) -> Result<String> = { Result.failure(IOException("нет ответа")) }

        /** Имена запрошенных операций по порядку. */
        val operations = mutableListOf<String>()

        override fun profileRevision(): Int = revision

        override suspend fun openURI(data: String, config: RepositoryUriConfig): Result<String> {
            operations += OPERATION.find(data)?.groupValues?.get(1).orEmpty()
            return respond(data)
        }

        /** Отвечает страницами альбома: номер страницы → тело ответа. */
        fun respondWithAlbumPages(totalPages: Int, page: (Int) -> String) {
            respond = { data ->
                val number = (1..totalPages).first { GraphQlRequest.pictureListInsideAlbum(ALBUM_ID, it) == data }
                Result.success(page(number))
            }
        }
    }

    private val base = ScriptedRepository()
    private val favorites = LusciousServerFavoritesRepositoryImpl(base)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mediaCategoriesFlow.value = null
    }

    @After
    fun tearDown() {
        mediaCategoriesFlow.value = null
        Dispatchers.resetMain()
    }

    // --- Поиск id картинки ---

    @Test
    fun `картинка с thumbnails null не роняет поиск id`() = runTest {
        base.respondWithAlbumPages(totalPages = 1) {
            albumPageJson(1, picture(11, "aaaa.jpg", ""","thumbnails":null""") + "," + picture(12, "photo_target1.jpg"))
        }

        val result = favorites.resolvePictureId(ALBUM_ID.toString(), "photo_target1.jpg")

        assertEquals("12", result.getOrNull())
    }

    @Test
    fun `элемент списка, который не объект, пропускается`() = runTest {
        base.respondWithAlbumPages(totalPages = 1) {
            albumPageJson(1, "null," + picture(12, "photo_target1.jpg"))
        }

        val result = favorites.resolvePictureId(ALBUM_ID.toString(), "photo_target1.jpg")

        assertEquals("12", result.getOrNull())
    }

    @Test
    fun `ответ без списка картинок — отказ, а не исключение`() = runTest {
        base.respond = { Result.success("""{"data":{"picture":null}}""") }

        val result = favorites.resolvePictureId(ALBUM_ID.toString(), "photo_target1.jpg")

        assertTrue(result.isFailure)
    }

    @Test
    fun `поиск id не обрывается на десятой странице альбома`() = runTest {
        base.respondWithAlbumPages(totalPages = 12) { page ->
            val name = if (page == 11) "photo_target1.jpg" else "other$page.jpg"
            albumPageJson(12, picture(page, name))
        }

        val result = favorites.resolvePictureId(ALBUM_ID.toString(), "photo_target1.jpg")

        assertEquals("11", result.getOrNull())
        assertEquals(11, base.operations.size)
    }

    @Test
    fun `обрезанный поиск сообщает, сколько страниц просмотрено`() = runTest {
        val totalPages = PICTURE_LOOKUP_MAX_PAGES + 50
        base.respondWithAlbumPages(totalPages) { page -> albumPageJson(totalPages, picture(page, "other$page.jpg")) }

        val error = favorites.resolvePictureId(ALBUM_ID.toString(), "photo_target1.jpg").exceptionOrNull()

        assertEquals(PICTURE_LOOKUP_MAX_PAGES, base.operations.size)
        assertEquals(
            "Картинка не найдена на первых $PICTURE_LOOKUP_MAX_PAGES страницах альбома из $totalPages",
            error?.message,
        )
    }

    // --- errors: null ---

    @Test
    fun `errors null в списке подписок — не ошибка`() = runTest {
        mediaCategoriesFlow.value = null
        base.respond = { data ->
            if ("MediaCategoriesBootstrap" in data) {
                Result.success(bootstrapJson(111))
            } else {
                Result.success("""{"data":{"favorite":{"list_by_date":{"errors":null,"picture_sets":{"items":[{"id":"7","title":"A"}]}}}}}""")
            }
        }

        val albums = favorites.getSubscribedAlbums(page = 1)

        assertEquals(listOf("7"), albums.getOrThrow().map { it.id })
    }

    @Test
    fun `errors null в списке лайков — не ошибка, а картинка без альбома остаётся без альбома`() = runTest {
        base.respond = { data ->
            if ("MediaCategoriesBootstrap" in data) {
                Result.success(bootstrapJson(111))
            } else {
                val item = """{"id":"5","url_to_original":"https://example.com/a.jpg","album":null}"""
                Result.success("""{"data":{"favorite":{"list_by_date":{"errors":null,"pictures":{"items":[$item]}}}}}""")
            }
        }

        val pictures = favorites.getServerLikedPictures(page = 1).getOrThrow()

        assertEquals(listOf("5"), pictures.map { it.id })
        assertNull("отсутствующий альбом записан строкой", pictures.single().album)
    }

    @Test
    fun `errors null в ответе мутации — успех`() = runTest {
        base.respond = { Result.success("""{"errors":null,"data":{"favorite":{"add_favorite":{"errors":null}}}}""") }

        val result = favorites.addFavorite(anchorId = "5", anchorType = "picture", favoriteType = "like")

        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
    }

    // --- Пользователь сессии ---

    @Test
    fun `id пользователя не берётся из справочника, загруженного под другим аккаунтом`() = runTest {
        // Справочник в памяти остался от прежнего аккаунта (или пришёл из кэша на диске).
        mediaCategoriesFlow.value = MediaCategories(filterSettings = FilterSettings(userId = 111))
        base.respond = { Result.success(bootstrapJson(0)) }

        val result = favorites.getSessionUserId()

        assertEquals("Вход в L не выполнен: сервер не назвал пользователя сессии", result.exceptionOrNull()?.message)
    }

    @Test
    fun `id пользователя запоминается до смены профиля и запрашивается заново после неё`() = runTest {
        base.respond = { Result.success(bootstrapJson(111)) }
        assertEquals("111", favorites.getSessionUserId().getOrNull())
        assertEquals("111", favorites.getSessionUserId().getOrNull())
        assertEquals("пока профиль тот же, сервер спрашивают один раз", 1, base.operations.size)

        // Вошли под другим аккаунтом.
        base.revision++
        base.respond = { Result.success(bootstrapJson(222)) }

        assertEquals("222", favorites.getSessionUserId().getOrNull())
        assertEquals(2, base.operations.size)
    }

    @Test
    fun `сбой сети при запросе пользователя сессии остаётся сбоем сети`() = runTest {
        mediaCategoriesFlow.value = MediaCategories(filterSettings = FilterSettings(userId = 111))
        base.respond = { Result.failure(IOException("обрыв")) }

        val error = favorites.getSessionUserId().exceptionOrNull()

        assertTrue("ожидался сбой сети, пришло $error", error is IOException)
    }
}
