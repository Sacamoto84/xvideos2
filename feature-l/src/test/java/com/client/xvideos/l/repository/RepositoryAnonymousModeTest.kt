package com.client.xvideos.l.repository

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.CloudflareChallengeTest.Companion.SERVER_ERROR_PAGE
import com.client.xvideos.l.anonymousGraphQlGetUrl
import com.client.xvideos.l.model.UserProfile
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Collections

/**
 * Анонимные запросы идут так же, как у сайта: GET на анонимный адрес без
 * cookies (Cloudflare кэширует такие ответы). Авторизованные — POST на
 * адрес участников, как раньше.
 */
class RepositoryAnonymousModeTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_l_anon").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }

        private const val QUERY = """{"operationName":"Q","query":"query Q { a }","variables":{}}"""
        private const val MUTATION = """{"operationName":"M","query":"mutation M { a }","variables":{}}"""
        private const val API_OK = """{"data":{"a":1}}"""
        private val CREDENTIALS = UserProfile(email = "user@example.com", password = "secret")

        /** Домен сайта без поддомена: cookie на него видна и участникам, и анониму. */
        private val SITE_DOMAIN = Url(LusciousEndpoints.API).host.substringAfter('.')
    }

    private data class Recorded(
        val method: HttpMethod,
        val url: Url,
        val cookie: String?,
        val requestedWith: String?,
        val body: String?,
    )

    private class Server(
        private val loginStatus: HttpStatusCode = HttpStatusCode.OK,
        private val loginBody: String = "<html>welcome</html>",
        private val loginCookie: String? = null,
    ) {
        val requests: MutableList<Recorded> = Collections.synchronizedList(mutableListOf())

        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/accounts/login/")) {
                val headers = if (loginCookie != null) {
                    headersOf(HttpHeaders.SetCookie, loginCookie)
                } else {
                    headersOf()
                }
                respond(loginBody, loginStatus, headers)
            } else {
                requests += Recorded(
                    method = request.method,
                    url = request.url,
                    cookie = request.headers[HttpHeaders.Cookie],
                    requestedWith = request.headers["X-Requested-With"],
                    body = (request.body as? TextContent)?.text,
                )
                respond(API_OK, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
    }

    private fun repository(server: Server, credentials: UserProfile = UserProfile()) = Repository(
        fileDb = AppFileDatabase(),
        credentials = { credentials },
        engineFactory = { server.engine },
        notifyAnonymousFallback = {},
    )

    @Test
    fun `без логина запрос идёт GET на анонимный адрес в формате сайта`() = runBlocking {
        val server = Server()

        val result = repository(server).openURI(QUERY)

        assertEquals(API_OK, result.getOrNull())
        val request = server.requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals(anonymousGraphQlGetUrl(LusciousEndpoints.API_ANONYMOUS, QUERY), request.url.toString())
        assertEquals("XMLHttpRequest", request.requestedWith)
        assertNull(request.cookie)
    }

    @Test
    fun `после успешного входа запрос идёт POST на адрес участников с cookie`() = runBlocking {
        val server = Server(loginCookie = "sessionid=s1; Domain=$SITE_DOMAIN; Path=/")

        repository(server, CREDENTIALS).openURI(QUERY)

        val request = server.requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(LusciousEndpoints.API, request.url.toString())
        assertEquals(QUERY, request.body)
        assertTrue("ожидалась cookie сессии, пришло ${request.cookie}", request.cookie.orEmpty().contains("sessionid=s1"))
    }

    @Test
    fun `после неудачного входа запрос анонимный и без cookie`() = runBlocking {
        val server = Server(
            loginStatus = HttpStatusCode.InternalServerError,
            loginBody = SERVER_ERROR_PAGE,
            loginCookie = "csrftoken=c1; Domain=$SITE_DOMAIN; Path=/",
        )

        repository(server, CREDENTIALS).openURI(QUERY)

        val request = server.requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals(Url(LusciousEndpoints.API_ANONYMOUS).host, request.url.host)
        assertNull(request.cookie)
    }

    @Test
    fun `анонимная мутация идёт POST на анонимный адрес с operationName`() = runBlocking {
        val server = Server()

        repository(server).openURI(MUTATION)

        val request = server.requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(LusciousEndpoints.API_ANONYMOUS + "?operationName=M", request.url.toString())
        assertEquals(MUTATION, request.body)
        assertNull(request.cookie)
    }
}
