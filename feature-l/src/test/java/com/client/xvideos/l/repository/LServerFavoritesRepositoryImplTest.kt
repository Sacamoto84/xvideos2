package com.client.xvideos.l.repository

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.LServerErrorException
import com.client.xvideos.l.model.UserProfile
import com.client.xvideos.l.net.graphQl.FilterSettings
import com.client.xvideos.l.net.graphQl.GraphQlRequest
import com.client.xvideos.l.net.graphQl.MediaCategories
import com.client.xvideos.l.net.graphQl.mediaCategoriesFlow
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

        private fun bootstrapJson(userId: Long) = if (userId > 0) {
            """{"auth":{"state":"authenticated","user":{"id":"$userId"}}}"""
        } else {
            """{"auth":{"state":"anonymous","user":null}}"""
        }

        private fun albumPageJson(totalPages: Int, items: String) =
            """{"data":{"picture":{"list_in_album":{"info":{"total_pages":$totalPages},"items":[$items]}}}}"""

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

        override suspend fun getSiteApi(url: String): Result<String> {
            operations += url.removePrefix(LusciousEndpoints.REST)
            return respond(url)
        }

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
    fun `подписки загружаются из REST для подтверждённого пользователя`() = runTest {
        mediaCategoriesFlow.value = null
        base.respond = { data ->
            if (data.endsWith("site_bootstrap")) {
                Result.success(bootstrapJson(111))
            } else {
                Result.success("""{"items":[{"id":"7","title":"A"}]}""")
            }
        }

        val albums = favorites.getSubscribedAlbums(page = 1)

        assertEquals(listOf("7"), albums.getOrThrow().map { it.id })
        assertEquals(listOf("luscious/site_bootstrap", "favorite/list_by_date?user_id=111&kind=picture_set&page=1&show_likes=true"), base.operations)
        assertNull(mediaCategoriesFlow.value)
    }

    @Test
    fun `картинка без альбома в REST-ответе остаётся без альбома`() = runTest {
        base.respond = { data ->
            if (data.endsWith("site_bootstrap")) {
                Result.success(bootstrapJson(111))
            } else {
                val item = """{"id":"5","url_to_original":"https://example.com/a.jpg","album":null}"""
                Result.success("""{"items":[$item]}""")
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

    @Test
    fun `удалённый с сервера альбом попадает в поток удалённых, неудачное удаление — нет`() = runTest {
        val removed = mutableListOf<String>()
        backgroundScope.launch { favorites.removedAlbumIds.collect { removed += it } }
        runCurrent()

        base.respond = { Result.failure(IOException("нет сети")) }
        favorites.unlikeAlbum("7")
        base.respond = { Result.success("""{"data":{"favorite":{"remove_favorite":{"errors":["нет"]}}}}""") }
        favorites.unlikeAlbum("8")
        base.respond = { Result.success("""{"data":{"favorite":{"remove_favorite":{"errors":null}}}}""") }
        favorites.unlikePicture("9")
        favorites.unlikeAlbum("10")
        runCurrent()

        assertEquals(listOf("10"), removed)
    }

    // --- Пользователь сессии ---

    private class SessionServer(
        private val loginAtHome: Boolean = false,
        private val homeStatus: HttpStatusCode = HttpStatusCode.OK,
        private val accepts: (Int) -> Boolean,
    ) {
        var logins = 0
            private set
        val cookies = mutableListOf<String?>()
        var homeVisits = 0
            private set

        fun repository(): Repository {
            val profile = SavedProfile(UserProfile("user@example.com", "test-password"))
            val domain = Url(LusciousEndpoints.API).host.substringAfter('.')
            return Repository(
                fileDb = AppFileDatabase(),
                credentials = { profile.snapshot },
                engineFactory = {
                    MockEngine { request ->
                        if (request.url.encodedPath == "/login/") {
                            check(request.method == HttpMethod.Get)
                            return@MockEngine respond(
                                "<form></form>",
                                headers = headersOf(HttpHeaders.SetCookie, "form=open; Path=/accounts/login/")
                            )
                        }
                        val cookie = request.headers[HttpHeaders.Cookie]
                        if (request.url.encodedPath.endsWith("/accounts/login/")) {
                            check(cookie == "form=open") { "Вход должен использовать сессию открытой формы" }
                            logins++
                            if (loginAtHome) {
                                respond("""{"location":"/","form":{"fields":{},"errors":[]}}""")
                            } else {
                                respond(
                                    "<html>welcome</html>",
                                    headers = headersOf(HttpHeaders.SetCookie, "sessionid=s$logins; Domain=$domain; Path=/")
                                )
                            }
                        } else if (loginAtHome && request.url.encodedPath == "/") {
                            check(request.method == HttpMethod.Get)
                            homeVisits++
                            respond(
                                "<html>welcome</html>",
                                status = homeStatus,
                                headers = headersOf(HttpHeaders.SetCookie, "sessionid=s$logins; Domain=$domain; Path=/")
                            )
                        } else {
                            cookies += cookie
                            check(request.method == HttpMethod.Get)
                            val response = if (request.url.encodedPath.endsWith("/favorite/list_by_date")) {
                                """{"items":[]}"""
                            } else {
                                val session = cookie?.substringAfter("sessionid=s")?.toIntOrNull()
                                bootstrapJson(if (session != null && accepts(session)) 111 else 0)
                            }
                            respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                        }
                    }
                },
                notifyAnonymousFallback = {},
            )
        }
    }

    @Test
    fun `вход завершается переходом по адресу из ответа формы`() = runTest {
        val server = SessionServer(loginAtHome = true) { true }
        val favorites = LusciousServerFavoritesRepositoryImpl(server.repository())

        assertEquals("111", favorites.getSessionUserId().getOrNull())
        assertEquals(1, server.logins)
        assertEquals(1, server.homeVisits)
        assertEquals(listOf("sessionid=s1"), server.cookies)
    }

    @Test
    fun `сбой сервера на завершающем шаге входа остаётся сетевой ошибкой списка`() = runTest {
        val server = SessionServer(loginAtHome = true, homeStatus = HttpStatusCode.ServiceUnavailable) { false }
        val favorites = LusciousServerFavoritesRepositoryImpl(server.repository())

        val failure = favorites.getSubscribedAlbums(page = 1).exceptionOrNull()
        assertEquals(HttpStatusCode.ServiceUnavailable.value, (failure as? LServerErrorException)?.statusCode)
        assertEquals(1, server.logins)
        assertEquals(1, server.homeVisits)
    }

    @Test
    fun `первая страница проверяет сохранённую сессию и повторяет вход с новой cookie`() = runTest {
        var expired = false
        val server = SessionServer { session -> session > 1 || !expired }
        val favorites = LusciousServerFavoritesRepositoryImpl(server.repository())
        assertEquals("111", favorites.getSessionUserId().getOrNull())

        expired = true
        assertEquals(emptyList<Any>(), favorites.getSubscribedAlbums(page = 1).getOrThrow())

        assertEquals(2, server.logins)
        assertEquals(
            listOf("sessionid=s1", "sessionid=s1", "sessionid=s2", "sessionid=s2"),
            server.cookies,
        )
    }

    @Test
    fun `если новая сессия тоже отвергнута, соседняя вкладка не запускает вход по кругу`() = runTest {
        val server = SessionServer { false }
        val favorites = LusciousServerFavoritesRepositoryImpl(server.repository())

        assertTrue(favorites.getSubscribedAlbums(page = 1).exceptionOrNull() is LSessionRequiredException)
        assertTrue(favorites.getServerLikedPictures(page = 1).exceptionOrNull() is LSessionRequiredException)

        assertEquals(2, server.logins)
        assertEquals(listOf("sessionid=s1", "sessionid=s2", null), server.cookies)
    }

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
