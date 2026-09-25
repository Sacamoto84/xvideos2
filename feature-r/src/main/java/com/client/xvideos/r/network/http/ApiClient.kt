package com.client.xvideos.r.network.http

import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.r.network.json.RJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import okhttp3.ConnectionSpec
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object ApiClient {

    const val USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

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
     * Анонимный bearer-токен redgifs. Пишется только из [loginLocked]/[refreshToken]
     * под [tokenMutex], читается из любого потока — отсюда `@Volatile`.
     */
    @Volatile
    var bearerToken: String? = null
        private set

    @PublishedApi
    internal val bearerHeaderRef = AtomicReference<String?>(null)

    private val SUCCESS_UNIT = Result.success(Unit)
    private val tokenMutex = Mutex()

    @Serializable
    data class TokenResponse(@SerialName("token") val token: String)

    /**
     * Гарантирует наличие токена. При параллельных запросах без токена
     * логин выполняется ровно один раз (double-checked под [tokenMutex]).
     */
    @PublishedApi
    internal suspend fun ensureToken(): Result<Unit> {
        if (bearerToken != null) return SUCCESS_UNIT
        return tokenMutex.withLock {
            if (bearerToken != null) SUCCESS_UNIT
            else loginLocked().map { }
        }
    }

    /**
     * Принудительно обновляет токен после 401, но только если другой корутин
     * не успел его уже заменить (сравнение с [previousToken] под мьютексом),
     * иначе несколько параллельных 401 устроили бы шторм логинов.
     */
    @PublishedApi
    internal suspend fun refreshToken(previousToken: String?): Result<Unit> {
        return tokenMutex.withLock {
            if (bearerToken != previousToken) {
                SUCCESS_UNIT
            } else {
                bearerToken = null
                bearerHeaderRef.set(null)
                loginLocked().map { }
            }
        }
    }

    /** Выполняет логин. Вызывать только удерживая [tokenMutex]. */
    private suspend fun loginLocked(): Result<Boolean> {
        return try {
            Timber.d("Red ApiClient login()")
            val tokenResponse =
                client.get("https://api.redgifs.com/v2/auth/temporary").body<TokenResponse>()
            val token = tokenResponse.token
            bearerToken = token
            bearerHeaderRef.set("Bearer $token")
            Timber.d("Red ApiClient login() SUCCESS - token received")
            Result.success(true)
        } catch (e: CancellationException) {
            // Отмена корутины — не ошибка сети. Без этого catch она превращалась
            // в Result.failure и уезжала вызывающему как настоящий сбой логина.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Red ApiClient login() FAILED: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun login(): Result<Boolean> = tokenMutex.withLock { loginLocked() }

    /**
     * Общая обёртка авторизованного запроса: гарантирует токен, выполняет [perform],
     * а при 401 один раз обновляет токен и повторяет. Единая точка обработки ошибок
     * вместо четырёх копий retry-логики.
     */
    @PublishedApi
    internal suspend inline fun <T> withAuth(crossinline perform: suspend (token: String?) -> T): Result<T> {
        ensureToken().onFailure { return Result.failure(it) }
        return try {
            Result.success(perform(bearerToken))
        } catch (e: CancellationException) {
            // Экран закрыли посреди запроса — это не сбой сети. Раньше отмена
            // превращалась в Result.failure, и вызывающий показывал снекбар с
            // текстом отмены корутины уже на предыдущем экране.
            throw e
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Timber.w("Red ApiClient 401 Unauthorized, retrying login...")
                val previous = bearerToken
                if (refreshToken(previous).isSuccess) {
                    return try {
                        Result.success(perform(bearerToken))
                    } catch (e2: CancellationException) {
                        throw e2
                    } catch (e2: Exception) {
                        Timber.e(e2, "Red ApiClient request FAILED after retry")
                        Result.failure(e2)
                    }
                }
            }
            Timber.e(e, "Red ApiClient request FAILED")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Red ApiClient request FAILED")
            Result.failure(e)
        }
    }

    suspend inline fun <reified T> request(
        url: String,
        params: Map<String, String> = emptyMap(),
    ): Result<T> = withAuth { token ->
        val authHeader = bearerHeaderRef.get() ?: token?.let { "Bearer $it" }
        client.get(url) {
            if (authHeader != null) headers.append(HttpHeaders.Authorization, authHeader)
            if (params.isNotEmpty()) {
                params.forEach { (key, value) -> parameter(key, value) }
            }
        }.body()
    }

    suspend inline fun <reified T> request(
        route: Route,
        vararg params: Pair<String, Any> = emptyArray(),
    ): Result<T> = withAuth { token ->
        val authHeader = bearerHeaderRef.get() ?: token?.let { "Bearer $it" }
        client.get(route.url) {
            if (authHeader != null) headers.append(HttpHeaders.Authorization, authHeader)
            if (params.isNotEmpty()) {
                for ((key, value) in params) parameter(key, value)
            }
        }.body()
    }

    suspend fun requestText(
        route: Route,
        vararg params: Pair<String, Any> = emptyArray(),
    ): Result<String> = withAuth { token ->
        val authHeader = bearerHeaderRef.get() ?: token?.let { "Bearer $it" }
        client.get(route.url) {
            if (authHeader != null) headers.append(HttpHeaders.Authorization, authHeader)
            if (params.isNotEmpty()) {
                for ((key, value) in params) parameter(key, value)
            }
        }.bodyAsText()
    }

    suspend fun requestText(
        url: String,
        vararg params: Pair<String, Any> = emptyArray(),
    ): Result<String> = withAuth { token ->
        val authHeader = bearerHeaderRef.get() ?: token?.let { "Bearer $it" }
        client.get(url) {
            if (authHeader != null) headers.append(HttpHeaders.Authorization, authHeader)
            if (params.isNotEmpty()) {
                for ((key, value) in params) parameter(key, value)
            }
        }.bodyAsText()
    }
}
