package com.client.xvideos.r.network.http

import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.r.network.json.RJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ConnectionSpec
import java.util.concurrent.TimeUnit

/**
 * HTTP-клиент модуля RedGifs на базе Ktor и движка OkHttp.
 *
 * Инкапсулирует:
 * - Управление временным анонимным Bearer-токеном через [BearerAuth] (получение, кэширование, обновление при 401);
 * - DNS-over-HTTPS резолвинг через [AppDns];
 * - Настройку таймаутов, ретраев и стандартных HTTP-заголовков (Referer, Origin, UserAgent).
 */
object ApiClient {

    /** Заголовок User-Agent браузера для обхода Cloudflare и ограничений поставщика. */
    const val USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    /** Адрес выдачи временного анонимного токена. */
    private const val AUTH_URL = "https://api.redgifs.com/v2/auth/temporary"

    /**
     * Сконфигурированный экземпляр [HttpClient] на базе OkHttp:
     * - DNS через [AppDns];
     * - Modern TLS и Compatible TLS;
     * - 30 секунд таймауты (connect, read, write);
     * - Автоматический retry до 3 раз с экспоненциальной задержкой;
     * - JSON ContentNegotiation через [RJson].
     */
    val client = HttpClient(OkHttp) {
        engine {
            config {
                dns(AppDns)
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
                followRedirects(true)
                connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            }
        }
        install(ContentNegotiation) {
            json(RJson)
        }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 3)
            exponentialDelay()
            modifyRequest { if (it.url.pathSegments.contains("auth")) it.headers.remove(HttpHeaders.Authorization) }
        }
        defaultRequest {
            headers.append("Referer", "https://www.redgifs.com/")
            headers.append("Origin", "https://www.redgifs.com")
            headers.append(HttpHeaders.UserAgent, USER_AGENT)
            headers.append(HttpHeaders.Accept, "application/json, text/plain, */*")
            headers.append(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
            //headers.append(HttpHeaders.Range, "bytes=0-500000")
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
        expectSuccess = true
    }

    /**
     * DTO ответа авторизационного эндпоинта `/v2/auth/temporary`.
     *
     * @property token Временный токен доступа.
     */
    @Serializable
    data class TokenResponse(@SerialName("token") val token: String)

    /** Анонимный токен и выполнение запросов под ним, с обновлением после 401. */
    val auth = BearerAuth { client.get(AUTH_URL).body<TokenResponse>().token }

    /** Текущий анонимный bearer-токен. */
    val bearerToken: String? get() = auth.token

    /** Принудительно получает новый токен. Безопасен для параллельных вызовов. */
    suspend fun login(): Result<Boolean> = auth.login()

    /**
     * Выполняет типизированный GET-запрос по произвольному [url] с автоматической авторизацией.
     *
     * @param T Тип десериализуемого тела ответа.
     * @param url Полный URL-адрес запроса.
     * @param params Map query-параметров.
     */
    suspend inline fun <reified T> request(
        url: String,
        params: Map<String, String> = emptyMap(),
    ): Result<T> = auth.withAuth { token ->
        val authHeader = token?.let { "Bearer $it" }
        client.get(url) {
            if (authHeader != null) headers.append(HttpHeaders.Authorization, authHeader)
            if (params.isNotEmpty()) {
                params.forEach { (key, value) -> parameter(key, value) }
            }
        }.body()
    }

    /**
     * Выполняет типизированный GET-запрос по объекту [route] с автоматической авторизацией.
     *
     * @param T Тип десериализуемого тела ответа.
     * @param route Сконфигурированный объект [Route].
     * @param params Дополнительные параметры запроса.
     */
    suspend inline fun <reified T> request(
        route: Route,
        vararg params: Pair<String, Any> = emptyArray(),
    ): Result<T> = auth.withAuth { token ->
        val authHeader = token?.let { "Bearer $it" }
        client.get(route.url) {
            if (authHeader != null) headers.append(HttpHeaders.Authorization, authHeader)
            if (params.isNotEmpty()) {
                for ((key, value) in params) parameter(key, value)
            }
        }.body()
    }

    /**
     * Выполняет запрос по объекту [route] и возвращает сырой текст ответа [String].
     *
     * @param route Сконфигурированный объект [Route].
     * @param params Дополнительные query-параметры.
     */
    suspend fun requestText(
        route: Route,
        vararg params: Pair<String, Any> = emptyArray(),
    ): Result<String> = auth.withAuth { token ->
        val authHeader = token?.let { "Bearer $it" }
        client.get(route.url) {
            if (authHeader != null) headers.append(HttpHeaders.Authorization, authHeader)
            if (params.isNotEmpty()) {
                for ((key, value) in params) parameter(key, value)
            }
        }.bodyAsText()
    }

    /**
     * Выполняет запрос по произвольному [url] и возвращает сырой текст ответа [String].
     *
     * @param url Полный URL-адрес запроса.
     * @param params Дополнительные query-параметры.
     */
    suspend fun requestText(
        url: String,
        vararg params: Pair<String, Any> = emptyArray(),
    ): Result<String> = auth.withAuth { token ->
        val authHeader = token?.let { "Bearer $it" }
        client.get(url) {
            if (authHeader != null) headers.append(HttpHeaders.Authorization, authHeader)
            if (params.isNotEmpty()) {
                for ((key, value) in params) parameter(key, value)
            }
        }.bodyAsText()
    }
}
