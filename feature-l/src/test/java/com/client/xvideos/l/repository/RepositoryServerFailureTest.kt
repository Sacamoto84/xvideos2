package com.client.xvideos.l.repository

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.CloudflareChallengeTest.Companion.SERVER_ERROR_PAGE
import com.client.xvideos.l.LServerErrorException
import com.client.xvideos.l.model.UserProfile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

/**
 * Поведение [Repository] при отказе сервера L: HTTP 500 не выдаётся за
 * антибот-проверку, а неудачный вход не закрывает раздел целиком.
 */
class RepositoryServerFailureTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_l_server").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }

        private const val API_OK = """{"data":{"ok":true}}"""
        private const val WRONG_CREDENTIALS_PAGE =
            "<html><body>The username and/or password you specified are not correct.</body></html>"
        private val CREDENTIALS = UserProfile(email = "user@example.com", password = "secret")
    }

    /** Подставной сервер: отвечает на логин и на GraphQL заданными кодом и телом, считает вызовы. */
    private class FakeServer(
        private val loginStatus: HttpStatusCode = HttpStatusCode.OK,
        private val loginBody: String = "<html><body>welcome</body></html>",
        private val apiStatus: HttpStatusCode = HttpStatusCode.OK,
        private val apiBody: String = API_OK,
    ) {
        val loginCalls = AtomicInteger()
        val apiCalls = AtomicInteger()

        val engine = MockEngine { request ->
            val html = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8")
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            when {
                request.url.encodedPath.endsWith("/accounts/login/") -> {
                    loginCalls.incrementAndGet()
                    respond(loginBody, loginStatus, html)
                }
                else -> {
                    apiCalls.incrementAndGet()
                    respond(apiBody, apiStatus, if (apiBody.startsWith("{")) json else html)
                }
            }
        }
    }

    private fun repository(
        server: FakeServer,
        notices: MutableList<String> = mutableListOf(),
        credentials: () -> UserProfile = { UserProfile() },
    ) = Repository(
        fileDb = AppFileDatabase(),
        credentials = credentials,
        engineFactory = { server.engine },
        notifyAnonymousFallback = { notices += it },
    )

    @Test
    fun `HTTP 500 от API — ошибка сервера с одного запроса без антибот-кулдауна`() = runBlocking {
        val server = FakeServer(apiStatus = HttpStatusCode.InternalServerError, apiBody = SERVER_ERROR_PAGE)
        val repository = repository(server)

        val error = repository.openURI("{}").exceptionOrNull()

        assertTrue("ожидалась LServerErrorException, пришло $error", error is LServerErrorException)
        assertEquals("Сервер L недоступен (HTTP 500)", error?.message)
        assertEquals(1, server.apiCalls.get())
        assertFalse(repository.protectionUiState.value.active)
    }

    @Test
    fun `неудачный вход из-за 500 переводит в анонимный режим, запрос проходит`() = runBlocking {
        val server = FakeServer(loginStatus = HttpStatusCode.InternalServerError, loginBody = SERVER_ERROR_PAGE)
        val notices = mutableListOf<String>()
        val repository = repository(server, notices) { CREDENTIALS }

        val result = repository.openURI("{}")

        assertEquals(API_OK, result.getOrNull())
        assertEquals(1, notices.size)
        assertTrue(notices.single(), notices.single().contains("HTTP 500"))
    }

    @Test
    fun `после неудачного входа вход не повторяется на каждом запросе`() = runBlocking {
        val server = FakeServer(loginStatus = HttpStatusCode.InternalServerError, loginBody = SERVER_ERROR_PAGE)
        val notices = mutableListOf<String>()
        val repository = repository(server, notices) { CREDENTIALS }

        repository.openURI("{}")
        repository.openURI("{\"q\":2}")

        assertEquals(1, server.loginCalls.get())
        assertEquals(2, server.apiCalls.get())
        assertEquals(1, notices.size)
    }

    @Test
    fun `новые логин и пароль снова пробуют войти`() = runBlocking {
        val server = FakeServer(loginStatus = HttpStatusCode.InternalServerError, loginBody = SERVER_ERROR_PAGE)
        val notices = mutableListOf<String>()
        var credentials = CREDENTIALS
        val repository = repository(server, notices) { credentials }

        repository.openURI("{}")
        credentials = CREDENTIALS.withPassword("another")
        repository.openURI("{\"q\":2}")

        assertEquals(2, server.loginCalls.get())
        assertEquals(2, notices.size)
    }

    @Test
    fun `неверный пароль тоже переводит в анонимный режим с понятной причиной`() = runBlocking {
        val server = FakeServer(loginBody = WRONG_CREDENTIALS_PAGE)
        val notices = mutableListOf<String>()
        val repository = repository(server, notices) { CREDENTIALS }

        val result = repository.openURI("{}")

        assertTrue(result.isSuccess)
        assertTrue(notices.single(), notices.single().contains("неверный логин или пароль"))
    }

    @Test
    fun `успешный вход не показывает предупреждений`() = runBlocking {
        val server = FakeServer()
        val notices = mutableListOf<String>()
        val repository = repository(server, notices) { CREDENTIALS }

        val result = repository.openURI("{}")

        assertTrue(result.isSuccess)
        assertEquals(1, server.loginCalls.get())
        assertTrue(notices.isEmpty())
    }
}
