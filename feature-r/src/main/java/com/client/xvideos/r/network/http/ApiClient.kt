package com.client.xvideos.r.network.http

import com.client.xvideos.common.net.SiteMaintenanceInterceptor
import com.client.xvideos.common.net.SiteMaintenanceReporter
import com.client.xvideos.common.net.UserAgentProvider
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.auth.RAccountSession
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.json.RJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.TimeUnit

/**
 * HTTP-клиент модуля R на базе Ktor и движка OkHttp.
 *
 * Инкапсулирует:
 * - Использование сессии [RAccountSession] и гостевого токена [BearerAuth];
 * - DNS-over-HTTPS резолвинг через [AppDns];
 * - Настройку таймаутов, ретраев и стандартных HTTP-заголовков (Referer, Origin, UserAgent).
 */
object ApiClient {

    /**
     * Заголовок User-Agent браузера для обхода Cloudflare и ограничений
     * поставщика. Берётся из общего [UserAgentProvider], как у клиента L: своя
     * зашитая строка устаревала вместе с версией браузера.
     */
    val USER_AGENT: String get() = UserAgentProvider.defaultUserAgent

    /** Адрес выдачи временного анонимного токена. */
    private const val AUTH_URL = "https://api.redgifs.com/v2/auth/temporary"

    /**
     * Сконфигурированный экземпляр [HttpClient] на базе OkHttp:
     * - DNS через [AppDns];
     * - Modern TLS и Compatible TLS;
     * - 30 секунд таймауты (connect, read, write);
     * - Повторы только там, где следующая попытка может пройти ([installRRetry]);
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
                addNetworkInterceptor { chain ->
                    val request = chain.request()
                    val scopedRequest = if (request.url.isRedgifsApi()) request else {
                        request.newBuilder().removeHeader(HttpHeaders.Authorization).build()
                    }
                    chain.proceed(scopedRequest)
                }
                // Сайт на техобслуживании отвечает 503 — плашка вместо безликой ошибки.
                addInterceptor(SiteMaintenanceInterceptor { SiteMaintenanceReporter.report("R") })
            }
        }
        install(ContentNegotiation) {
            json(RJson)
        }
        installRRetry()
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

    @PublishedApi
    internal suspend fun <T> get(
        url: String,
        params: List<Pair<String, Any>>,
        read: suspend (HttpResponse) -> T,
    ): Result<T> {
        val token = if (url.toHttpUrlOrNull()?.isRedgifsApi() == true) RAccountSession.activeToken() else null
        if (token != null) {
            val result = getAsAccount(url, params, token, read)
            if ((result.exceptionOrNull() as? ClientRequestException)?.response?.status != HttpStatusCode.Unauthorized) {
                return result
            }
            // Публичную выдачу можно загрузить гостем. Отказ отдельного эндпоинта
            // сам по себе не должен сбрасывать вход в аккаунт.
        }
        return getAsGuest(url, params, read)
    }

    private suspend fun <T> getAsAccount(
        url: String,
        params: List<Pair<String, Any>>,
        token: String,
        read: suspend (HttpResponse) -> T,
    ): Result<T> = runCatchingCancellable { performGet(url, params, token, read) }

    private suspend fun <T> getAsGuest(
        url: String,
        params: List<Pair<String, Any>>,
        read: suspend (HttpResponse) -> T,
    ): Result<T> = auth.withAuth { token -> performGet(url, params, token, read) }

    private suspend fun <T> performGet(
        url: String,
        params: List<Pair<String, Any>>,
        token: String?,
        read: suspend (HttpResponse) -> T,
    ): T {
        val response = client.get(url) {
            if (token != null) headers.append(HttpHeaders.Authorization, "Bearer $token")
            for ((key, value) in params) parameter(key, value)
        }
        return read(response)
    }

    internal suspend fun <T> requestAsGuest(route: Route, read: suspend (HttpResponse) -> T): Result<T> =
        getAsGuest(route.url, emptyList(), read)

    internal suspend fun <T> requestAsAccount(
        route: Route,
        account: RAccountStatus.SignedIn? = null,
        body: JsonElement? = null,
        read: suspend (HttpResponse) -> T,
    ): Result<T> {
        val url = route.url
        if (url.toHttpUrlOrNull()?.isRedgifsApi() != true) {
            return Result.failure(IllegalArgumentException("Unsupported R API URL: $url"))
        }
        val authorization = RAccountSession.authorization(account) ?: return missingAccountToken(account)
        val result = performAccountRequest(route, authorization.token, body, read)
        if ((result.exceptionOrNull() as? ClientRequestException)?.response?.status != HttpStatusCode.Unauthorized) {
            return result
        }
        val renewed = RAccountSession.authorization(authorization.account, rejectedToken = authorization.token)
            ?: return missingAccountToken(authorization.account)
        return performAccountRequest(route, renewed.token, body, read)
    }

    private fun <T> missingAccountToken(account: RAccountStatus.SignedIn?): Result<T> {
        val signedIn = RAccountSession.status.value as? RAccountStatus.SignedIn
        val message = if (signedIn != null && (account == null || signedIn == account)) {
            "Не удалось обновить сессию R. Повторите попытку."
        } else {
            "Войдите в R"
        }
        return Result.failure(IllegalStateException(message))
    }

    private suspend fun <T> performAccountRequest(
        route: Route,
        token: String,
        body: JsonElement?,
        read: suspend (HttpResponse) -> T,
    ): Result<T> {
        if (route.isGet) return getAsAccount(route.url, emptyList(), token, read)
        return runCatchingCancellable {
            val response = client.request(route.url) {
                method = HttpMethod.parse(route.method)
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            read(response)
        }
    }

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
    ): Result<T> = get(url, params.toList()) { it.body() }

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
    ): Result<T> = get(route.url, params.toList()) { it.body() }

    /**
     * Выполняет запрос по объекту [route] и возвращает сырой текст ответа [String].
     *
     * @param route Сконфигурированный объект [Route].
     * @param params Дополнительные query-параметры.
     */
    suspend fun requestText(
        route: Route,
        vararg params: Pair<String, Any> = emptyArray(),
    ): Result<String> = get(route.url, params.toList()) { it.bodyAsText() }

    /**
     * Выполняет запрос по произвольному [url] и возвращает сырой текст ответа [String].
     *
     * @param url Полный URL-адрес запроса.
     * @param params Дополнительные query-параметры.
     */
    suspend fun requestText(
        url: String,
        vararg params: Pair<String, Any> = emptyArray(),
    ): Result<String> = get(url, params.toList()) { it.bodyAsText() }
}

internal fun HttpUrl.isRedgifsApi(): Boolean =
    isHttps && host == "api.redgifs.com" && port == 443 && username.isEmpty() && password.isEmpty()
