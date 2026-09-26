package com.client.xvideos.l

import com.client.xvideos.common.AppBuildInfo
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.l.repository.LusciousEndpoints.LOGIN
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
import io.ktor.serialization.kotlinx.json.json
import com.client.xvideos.l.net.json.LJson
import timber.log.Timber
import java.io.IOException

/**
 * Низкоуровневый HTTP-клиент на Ktor/OkHttp для взаимодействия с Luscious.
 *
 * Обеспечивает:
 * - Безопасный DNS-over-HTTPS резолвинг через [AppDns].
 * - Сохранение сессионных cookies между запросами ([AcceptAllCookiesStorage]).
 * - Автоматический повтор запросов при сетевых сбоях и ошибках 413, 429, 500-504 ([HttpRequestRetry]).
 * - Аутентификацию пользователя через веб-форму логина ([login]) с детекцией Cloudflare challenge.
 *
 * @property timeoutMillis Таймаут соединения и чтения в миллисекундах (по умолчанию 15 000).
 * @property maxRetries Максимальное число повторов при ошибках (по умолчанию 5).
 * @property retryStatusCodes Набор кодов ответа HTTP, требующих повтора запроса.
 * @property backoffFactor Множитель экспоненциальной задержки между повторами (мс).
 * @param username Логин пользователя.
 * @param password Пароль пользователя.
 */
class KtorRequestHandler(
    private val timeoutMillis: Long = 15000,
    private val maxRetries: Int = 5,
    private val retryStatusCodes: Set<Int> = setOf(413, 429, 500, 502, 503, 504),
    private val backoffFactor: Long = 1000,
    username: String? = null,
    password: String? = null
) {
    private val userAgent = UserAgentProvider.randomDesktopBrowser()
    @Volatile
    private var username: String? = username
    @Volatile
    private var password: String? = password

    /** Экземпляр HttpClient со сконфигурированным движком OkHttp. */
    val client = HttpClient(OkHttp) {
        engine {
            config {
                dns(AppDns)
            }
        }

        install(ContentNegotiation) { json(LJson) }

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
        if (AppBuildInfo.debug) {
            install(Logging) { level = LogLevel.HEADERS }
        }
    }

    /**
     * Выполняет HTTP GET-запрос и возвращает ответ в виде строки.
     *
     * @param url Целевой URL запроса.
     * @param params Параметры строки запроса (query parameters).
     */
    suspend fun get(url: String, params: Map<String, String> = emptyMap()): String {
        return client.get {
            url(url)
            if (params.isNotEmpty()) {
                params.forEach { (k, v) -> parameter(k, v) }
            }
        }.body()
    }

    /**
     * Выполняет HTTP POST-запрос с телом в формате `application/json` (используется для GraphQL).
     *
     * @param url Целевой URL GraphQL endpoint.
     * @param data Сырая строка JSON запроса.
     */
    suspend fun postJson(url: String, data: String): String {
        return client.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(TextContent(data, ContentType.Application.Json))
        }.body()
    }

    /**
     * Выполняет HTTP POST-запрос формы `application/x-www-form-urlencoded`.
     */
    private suspend fun post(url: String, formData: Map<String, String> = emptyMap()): String {
        return client.post {
            url(url)
            setBody(FormDataContent(Parameters.build {
                formData.forEach { (k, v) -> append(k, v) }
            }))
        }.body()
    }


    // --- Login ---
    /** Флаг успешной аутентификации в текущей сессии клиента. */
    var loggedIn: Boolean = false
        private set

    /** `true`, если клиент успешно аутентифицирован. */
    val isLoggedIn: Boolean get() = loggedIn

    /** `true`, если заданы логин и пароль. */
    val hasCredentials: Boolean get() = !username.isNullOrBlank() && !password.isNullOrBlank()

    /**
     * Обновляет учетные данные пользователя. При изменении логина или пароля статус входа сбрасывается.
     */
    fun setCredentials(username: String?, password: String?) {
        val normalizedUsername = username?.trim().orEmpty()
        val normalizedPassword = password.orEmpty()
        if (this.username != normalizedUsername || this.password != normalizedPassword) {
            loggedIn = false
            this.username = normalizedUsername
            this.password = normalizedPassword
        }
    }

    /**
     * Закрывает HTTP-клиент и освобождает сетевые ресурсы.
     */
    fun close() {
        client.close()
    }

    /**
     * Выполняет вход на сайт Luscious, отправляя учетные данные на endpoint логина.
     *
     * @return `true`, если вход выполнен успешно.
     */
    suspend fun login(): Boolean {
        val currentUsername = username
        val currentPassword = password
        if (currentUsername.isNullOrBlank() || currentPassword.isNullOrBlank()) {
            Timber.w("L login: username or password not provided")
            loggedIn = false
            return false
        }

        val formData = mapOf(
            "login" to currentUsername,
            "password" to currentPassword,
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

    /** Проверяет, является ли ответ страницей верификации/капчи Cloudflare. */
    private fun String.isCloudflareChallenge(): Boolean {
        if (isEmpty()) return false
        return contains("<title>Just a moment", ignoreCase = true) ||
                contains("challenge-platform", ignoreCase = true) ||
                contains("cf-chl", ignoreCase = true)
    }
    // ! --- Login --- !

}
