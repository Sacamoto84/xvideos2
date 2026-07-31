package com.client.xvideos.l

import com.client.xvideos.BuildConfig
import com.client.xvideos.l.net.Luscious.Companion.LOGIN
import com.client.xvideos.common.net.UserAgentProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.coroutines.CancellationException
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.serialization.gson.gson
import timber.log.Timber
import java.io.IOException

class KtorRequestHandler(
    private val timeoutMillis: Long = 15000,
    private val maxRetries: Int = 5,
    private val retryStatusCodes: Set<Int> = setOf(413, 429, 500, 502, 503, 504),
    private val backoffFactor: Long = 1000,
    username: String? = null,
    password: String? = null
) {
    private val userAgent = UserAgentProvider.randomDesktopBrowser()
    private var username: String? = username
    private var password: String? = password

    val client = HttpClient(OkHttp) {

        install(ContentNegotiation) { gson() }

        // Подключаем поддержку куков (сохраняет cookies между запросами)
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }

        // Единый слой ретраев: и на retryable-статусы, и на сетевые ошибки.
        // Использует параметры конструктора, чтобы не было расхождения настроек.
        install(HttpRequestRetry) {
            maxRetries = this@KtorRequestHandler.maxRetries
            retryIf { _, response -> response.status.value in retryStatusCodes }
            retryOnExceptionIf { _, cause -> cause is IOException }
            delayMillis { attempt -> backoffFactor * attempt }  // backoff = backoffFactor * номер попытки
        }

        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
            socketTimeoutMillis = timeoutMillis
        }

        defaultRequest { headers.append(HttpHeaders.UserAgent, userAgent) }

        // ВАЖНО: при LogLevel.ALL Ktor пишет тело и заголовки запросов (включая
        // login/password и session-cookie) в лог. Подробное логирование оставляем
        // только в debug-сборках, чтобы не утекали учётные данные в release.
        if (BuildConfig.DEBUG) {
            install(Logging) { level = LogLevel.HEADERS }
        }
    }

    suspend fun get(url: String, params: Map<String, String> = emptyMap()): String {
        return client.get {
            url(url)
            params.forEach { (k, v) -> parameter(k, v) }
        }.body()
    }

    suspend fun postJson(url: String, data: String): String {
        return client.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(TextContent(data, ContentType.Application.Json))
        }.body()
    }

    private suspend fun post(url: String, formData: Map<String, String> = emptyMap()): String {
        return client.post {
            url(url)
            setBody(FormDataContent(Parameters.build {
                formData.forEach { (k, v) -> append(k, v) }
            }))
        }.body()
    }


    // --- Login ---
    var loggedIn: Boolean = false
        private set

    fun setCredentials(username: String?, password: String?) {
        val normalizedUsername = username?.trim().orEmpty()
        val normalizedPassword = password.orEmpty()
        if (this.username != normalizedUsername || this.password != normalizedPassword) {
            loggedIn = false
            this.username = normalizedUsername
            this.password = normalizedPassword
        }
    }

    fun close() {
        client.close()
    }

    suspend fun login(): Boolean {
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            Timber.w("L login: username or password not provided")
            loggedIn = false
            return false
        }

        val formData = mapOf(
            "login" to username.orEmpty(),
            "password" to password.orEmpty(),
            "remember" to "on"
        )

        val response = try {
            post(LOGIN, formData)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "L login request failed")
            loggedIn = false
            return false
        }

        if (response.isCloudflareChallenge()) {
            Timber.w("L login blocked by Cloudflare challenge")
            loggedIn = false
            return false
        }

        loggedIn = if ("The username and/or password you specified are not correct." in response) {
            Timber.w("L login failed: please check your credentials")
            false
        } else {
            Timber.i("L login successful")
            true
        }
        return loggedIn
    }

    private fun String.isCloudflareChallenge(): Boolean {
        return contains("<title>Just a moment", ignoreCase = true) ||
                contains("challenge-platform", ignoreCase = true) ||
                contains("cf-chl", ignoreCase = true)
    }
    // ! --- Login --- !

}
