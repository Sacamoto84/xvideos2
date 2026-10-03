package com.client.xvideos.l.repository

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.CloudflareChallengeTest.Companion.SERVER_ERROR_PAGE
import com.client.xvideos.l.LServerErrorException
import com.client.xvideos.l.model.UserProfile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.file.Files
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Поведение [Repository] при сбоях сети: временный отказ входа не оставляет
 * сессию анонимной до перезапуска, а повторы запроса не держат очередь раздела.
 */
class RepositoryNetworkFailureTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_l_network").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }

        private const val API_OK = """{"data":{"a":1}}"""
        private const val WRONG_CREDENTIALS_PAGE =
            "<html><body>The username and/or password you specified are not correct.</body></html>"
        private val CREDENTIALS = UserProfile(email = "user@example.com", password = "secret")
        private val JSON = headersOf(HttpHeaders.ContentType, "application/json")
        private val HTML = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8")

        /** Пауза перед повтором в тестах: больше интервала между запросами, но не секунды. */
        private const val TEST_BACKOFF_MS = 500L

        private val OPERATION_IN_BODY = Regex("\"operationName\":\"([A-Za-z0-9_]+)\"")

        private fun query(name: String) = """{"operationName":"$name","query":"query $name { a }","variables":{}}"""
    }

    /** Подставной сервер: вход и API отвечают так, как велит тест; вызовы записываются. */
    private class FakeServer {
        @Volatile
        var login: suspend MockRequestHandleScope.() -> HttpResponseData = LOGIN_OK

        @Volatile
        var api: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
            { respond(API_OK, HttpStatusCode.OK, JSON) }

        val loginCalls = AtomicInteger()
        val apiCalls: MutableList<HttpRequestData> = Collections.synchronizedList(mutableListOf())

        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/accounts/login/")) {
                loginCalls.incrementAndGet()
                login()
            } else {
                apiCalls += request
                api(request)
            }
        }

        companion object {
            val LOGIN_OK: suspend MockRequestHandleScope.() -> HttpResponseData =
                { respond("<html><body>welcome</body></html>", HttpStatusCode.OK, HTML) }
        }
    }

    /** Имя операции запроса: у анонимного GET оно в адресе, у POST вошедшего — в теле. */
    private fun HttpRequestData.operation(): String? =
        url.parameters["operationName"]
            ?: OPERATION_IN_BODY.find((body as? TextContent)?.text.orEmpty())?.groupValues?.get(1)

    /** Время для [Repository]: тест двигает его сам, чтобы не ждать паузу повторного входа. */
    private var now = 1_000_000L

    private fun repository(
        server: FakeServer,
        notices: MutableList<String> = mutableListOf(),
        backoffMs: Long = TEST_BACKOFF_MS,
        credentials: () -> UserProfile = { UserProfile() },
    ) = Repository(
        fileDb = AppFileDatabase(),
        credentials = credentials,
        engineFactory = { server.engine },
        notifyAnonymousFallback = { notices += it },
        nowMs = { now },
        retryBackoffMs = backoffMs,
    )

    // --- Вход ---

    @Test
    fun `после сбоя сети при входе вход повторяется, когда пауза прошла`() = runBlocking {
        val server = FakeServer().apply { login = { throw UnknownHostException("нет сети") } }
        val repository = repository(server) { CREDENTIALS }

        repository.openURI(query("A"))
        server.login = FakeServer.LOGIN_OK
        now += L_LOGIN_RETRY_INTERVAL_MS
        repository.openURI(query("B"))

        assertEquals(2, server.loginCalls.get())
        assertEquals(LusciousEndpoints.API, server.apiCalls.last().url.toString())
    }

    @Test
    fun `до конца паузы вход после сбоя сети не повторяется`() = runBlocking {
        val server = FakeServer().apply { login = { throw UnknownHostException("нет сети") } }
        val repository = repository(server) { CREDENTIALS }

        repository.openURI(query("A"))
        now += L_LOGIN_RETRY_INTERVAL_MS - 1
        repository.openURI(query("B"))

        assertEquals(1, server.loginCalls.get())
    }

    @Test
    fun `после отказа сервера при входе вход повторяется, когда пауза прошла`() = runBlocking {
        val server = FakeServer().apply {
            login = { respond(SERVER_ERROR_PAGE, HttpStatusCode.InternalServerError, HTML) }
        }
        val repository = repository(server) { CREDENTIALS }

        repository.openURI(query("A"))
        now += L_LOGIN_RETRY_INTERVAL_MS
        repository.openURI(query("B"))

        assertEquals(2, server.loginCalls.get())
    }

    @Test
    fun `отвергнутые логин и пароль не пробуются снова и после паузы`() = runBlocking {
        val server = FakeServer().apply {
            login = { respond(WRONG_CREDENTIALS_PAGE, HttpStatusCode.OK, HTML) }
        }
        val repository = repository(server) { CREDENTIALS }

        repository.openURI(query("A"))
        now += L_LOGIN_RETRY_INTERVAL_MS
        repository.openURI(query("B"))

        assertEquals(1, server.loginCalls.get())
    }

    @Test
    fun `повторный сбой входа не показывает второе предупреждение`() = runBlocking {
        val server = FakeServer().apply { login = { throw UnknownHostException("нет сети") } }
        val notices = mutableListOf<String>()
        val repository = repository(server, notices) { CREDENTIALS }

        repository.openURI(query("A"))
        now += L_LOGIN_RETRY_INTERVAL_MS
        repository.openURI(query("B"))

        assertEquals(1, notices.size)
    }

    // --- Кэш в памяти ---

    @Test
    fun `после входа ответ, закэшированный анонимно, запрашивается заново`() = runBlocking {
        val server = FakeServer().apply { login = { throw UnknownHostException("нет сети") } }
        val repository = repository(server) { CREDENTIALS }

        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)
        server.login = FakeServer.LOGIN_OK
        now += L_LOGIN_RETRY_INTERVAL_MS
        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)

        assertEquals(2, server.apiCalls.size)
    }

    @Test
    fun `после смены логина ответ прежнего аккаунта запрашивается заново`() = runBlocking {
        val server = FakeServer()
        var credentials = CREDENTIALS
        val repository = repository(server) { credentials }

        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)
        credentials = CREDENTIALS.copy(email = "other@example.com")
        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)

        assertEquals(2, server.apiCalls.size)
    }

    @Test
    fun `после выхода ответ вошедшего пользователя запрашивается заново`() = runBlocking {
        val server = FakeServer()
        var credentials = CREDENTIALS
        val repository = repository(server) { credentials }

        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)
        repository.logout()
        credentials = UserProfile()
        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)

        assertEquals(2, server.apiCalls.size)
    }

    @Test
    fun `без смены режима повторный запрос берётся из кэша`() = runBlocking {
        val server = FakeServer()
        val repository = repository(server)

        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)
        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)

        assertEquals(1, server.apiCalls.size)
    }

    @Test
    fun `ответ запроса, начатого до входа, после входа в кэш не попадает`() = runBlocking {
        val anonymousSent = CompletableDeferred<Unit>()
        val answerAnonymous = CompletableDeferred<Unit>()
        val held = AtomicBoolean()
        val server = FakeServer().apply {
            login = { throw UnknownHostException("нет сети") }
            api = { request ->
                if (request.operation() == "A" && held.compareAndSet(false, true)) {
                    anonymousSent.complete(Unit)
                    answerAnonymous.await()
                }
                respond(API_OK, HttpStatusCode.OK, JSON)
            }
        }
        val repository = repository(server) { CREDENTIALS }

        // Анонимный запрос ушёл и ждёт ответа.
        val inFlight = async(Dispatchers.Default) { repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM) }
        anonymousSent.await()
        // Соседний запрос тем временем выполняет вход — кэш при этом очищается.
        server.login = FakeServer.LOGIN_OK
        now += L_LOGIN_RETRY_INTERVAL_MS
        val afterLogin = async(Dispatchers.Default) { repository.openURI(query("B")) }
        while (server.loginCalls.get() < 2) delay(10)
        delay(200)
        // Анонимный ответ приходит уже после очистки.
        answerAnonymous.complete(Unit)
        inFlight.await()
        afterLogin.await()

        repository.openURI(query("A"), RepositoryUriConfig.CACHE_RAM)

        assertEquals(
            "анонимный ответ остался в кэше и отдан вошедшему",
            2,
            server.apiCalls.count { it.operation() == "A" },
        )
    }

    // --- Повторы запроса ---

    @Test
    fun `без сети ошибка приходит с первой попытки`() = runBlocking {
        val server = FakeServer().apply { api = { throw UnknownHostException("нет сети") } }

        val error = repository(server).openURI(query("A")).exceptionOrNull()

        assertTrue("ожидалась UnknownHostException, пришло $error", error is UnknownHostException)
        assertEquals(1, server.apiCalls.size)
    }

    @Test
    fun `таймаут соединения не повторяется`() = runBlocking {
        val server = FakeServer().apply { api = { throw SocketTimeoutException("timeout") } }

        val error = repository(server).openURI(query("A")).exceptionOrNull()

        assertTrue("ожидалась SocketTimeoutException, пришло $error", error is SocketTimeoutException)
        assertEquals(1, server.apiCalls.size)
    }

    @Test
    fun `обрыв соединения повторяется, и запрос проходит`() = runBlocking {
        val failed = AtomicBoolean()
        val server = FakeServer().apply {
            api = {
                if (failed.compareAndSet(false, true)) throw IOException("Connection reset")
                respond(API_OK, HttpStatusCode.OK, JSON)
            }
        }

        val result = repository(server).openURI(query("A"))

        assertEquals(API_OK, result.getOrNull())
        assertEquals(2, server.apiCalls.size)
    }

    @Test
    fun `502 на всех попытках — ошибка сервера после шести запросов`() = runBlocking {
        val server = FakeServer().apply {
            api = { respond(SERVER_ERROR_PAGE, HttpStatusCode.BadGateway, HTML) }
        }

        val error = repository(server, backoffMs = 1).openURI(query("A")).exceptionOrNull()

        assertTrue("ожидалась LServerErrorException, пришло $error", error is LServerErrorException)
        assertEquals(6, server.apiCalls.size)
    }

    @Test
    fun `пауза перед повтором не задерживает соседний запрос`() = runBlocking {
        val failed = AtomicBoolean()
        val firstAnswered = CompletableDeferred<Unit>()
        val server = FakeServer().apply {
            api = { request ->
                val isFirst = request.url.parameters["operationName"] == "A" && failed.compareAndSet(false, true)
                if (isFirst) {
                    firstAnswered.complete(Unit)
                    respond(SERVER_ERROR_PAGE, HttpStatusCode.BadGateway, HTML)
                } else {
                    respond(API_OK, HttpStatusCode.OK, JSON)
                }
            }
        }
        val repository = repository(server)

        val first = async(Dispatchers.Default) { repository.openURI(query("A")) }
        firstAnswered.await()
        val second = async(Dispatchers.Default) { repository.openURI(query("B")) }

        assertTrue(first.await().isSuccess)
        assertTrue(second.await().isSuccess)
        assertEquals(listOf("A", "B", "A"), server.apiCalls.map { it.url.parameters["operationName"] })
    }
}
