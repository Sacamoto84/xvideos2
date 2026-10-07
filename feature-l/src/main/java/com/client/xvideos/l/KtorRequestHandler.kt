package com.client.xvideos.l

import com.client.xvideos.common.AppBuildInfo
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.l.repository.LusciousEndpoints.LOGIN
import com.client.xvideos.common.net.UserAgentProvider
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.coroutines.CancellationException
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import com.client.xvideos.l.net.json.LJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import timber.log.Timber

/**
 * Низкоуровневый HTTP-клиент на Ktor/OkHttp для взаимодействия с L.
 *
 * Обеспечивает:
 * - Безопасный DNS-over-HTTPS резолвинг через [AppDns].
 * - Сохранение сессионных cookies между запросами ([AcceptAllCookiesStorage]).
 * - Отделение отказа сервера (5xx, [LServerErrorException]) от проверки Cloudflare ([LHttpResponse]).
 * - Аутентификацию пользователя через веб-форму логина ([login]) с детекцией Cloudflare challenge.
 *
 * Повторов здесь нет: каждый метод — одна попытка. Повторяет
 * [Repository][com.client.xvideos.l.repository.Repository], и пауза перед
 * повтором идёт вне его очереди запросов. Раньше повторял плагин Ktor внутри
 * запроса, и один неудачный запрос держал всю очередь раздела.
 *
 * @property timeoutMillis Таймаут соединения и чтения в миллисекундах (по умолчанию 15 000).
 * @param username Логин пользователя.
 * @param password Пароль пользователя.
 * @param engineOverride Движок HTTP для тестов; `null` — боевой OkHttp с DNS-over-HTTPS.
 */
class KtorRequestHandler(
    private val timeoutMillis: Long = 15000,
    username: String? = null,
    password: String? = null,
    engineOverride: HttpClientEngine? = null,
) {
    private val userAgent = UserAgentProvider.randomDesktopBrowser()
    @Volatile
    private var username: String? = username
    @Volatile
    private var password: String? = password

    /** Клиент с cookies: вход и запросы вошедшего пользователя. */
    val client = createClient(engineOverride, withCookies = true)

    /**
     * Клиент без cookies для анонимных запросов — как `credentials: "omit"` у
     * сайта. Отдельный экземпляр, потому что плагин cookies не отключается на
     * один запрос, а после неудачного входа в хранилище могут остаться cookies
     * на весь домен.
     */
    private val anonymousClient = createClient(engineOverride, withCookies = false)

    /** OkHttp с DNS-over-HTTPS либо подставной движок из конструктора. */
    private fun createClient(engineOverride: HttpClientEngine?, withCookies: Boolean): HttpClient =
        if (engineOverride != null) {
            HttpClient(engineOverride) { configure(withCookies) }
        } else {
            HttpClient(OkHttp) {
                engine {
                    config {
                        dns(AppDns)
                    }
                }
                configure(withCookies)
            }
        }

    /** Общая настройка клиента для боевого и подставного движков. */
    private fun HttpClientConfig<*>.configure(withCookies: Boolean) {
        install(ContentNegotiation) { json(LJson) }

        // Подключаем поддержку куков (сохраняет cookies между запросами)
        if (withCookies) {
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
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
     * Выполняет HTTP POST-запрос с телом в формате `application/json` (используется для GraphQL).
     *
     * @param url Целевой URL GraphQL endpoint.
     * @param data Сырая строка JSON запроса.
     */
    suspend fun postJson(url: String, data: String): LHttpResponse {
        return client.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(TextContent(data, ContentType.Application.Json))
        }.toLResponse()
    }

    /**
     * Анонимный GraphQL-запрос так же, как его шлёт сайт: GET с запросом в URL
     * ([anonymousGraphQlGetUrl]), без cookies, с `X-Requested-With`. Такие
     * ответы кэширует Cloudflare. Мутации и слишком длинные запросы идут POST
     * на тот же адрес с `?operationName=` — тоже как у сайта и тоже без cookies.
     *
     * @param endpoint Анонимный GraphQL endpoint.
     * @param data Сырая строка JSON запроса.
     */
    suspend fun graphQlAnonymous(endpoint: String, data: String): LHttpResponse {
        val getUrl = anonymousGraphQlGetUrl(endpoint, data)
        val response = if (getUrl != null) {
            anonymousClient.get(getUrl) {
                header(REQUESTED_WITH_HEADER, REQUESTED_WITH_XHR)
            }
        } else {
            anonymousClient.post(anonymousGraphQlPostUrl(endpoint, data)) {
                header(REQUESTED_WITH_HEADER, REQUESTED_WITH_XHR)
                contentType(ContentType.Application.Json)
                setBody(TextContent(data, ContentType.Application.Json))
            }
        }
        return response.toLResponse()
    }

    private suspend fun HttpResponse.toLResponse(): LHttpResponse {
        val body = bodyAsText()
        return LHttpResponse(status.value, body, isCloudflareChallenge(headers[CF_MITIGATED_HEADER], body))
    }

    /**
     * Выполняет HTTP POST-запрос формы `application/x-www-form-urlencoded`.
     */
    private suspend fun post(url: String, formData: Map<String, String> = emptyMap()): HttpResponse {
        return client.post {
            url(url)
            setBody(FormDataContent(Parameters.build {
                formData.forEach { (k, v) -> append(k, v) }
            }))
        }
    }

    // --- Login ---
    /**
     * Флаг успешной аутентификации в текущей сессии клиента. `@Volatile`:
     * [Repository][com.client.xvideos.l.repository.Repository] читает его вне
     * мьютекса входа, выбирая между анонимным и авторизованным запросом.
     */
    @Volatile
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
     * Закрывает HTTP-клиенты и освобождает сетевые ресурсы.
     */
    fun close() {
        client.close()
        anonymousClient.close()
    }

    /**
     * Выполняет вход на сайт L, отправляя учетные данные на endpoint логина.
     *
     * @return успех либо отказ с причиной для пользователя в `message`:
     * сеть, проверка Cloudflare, [LServerErrorException], неверные логин или пароль.
     */
    suspend fun login(): Result<Unit> {
        val currentUsername = username
        val currentPassword = password
        if (currentUsername.isNullOrBlank() || currentPassword.isNullOrBlank()) {
            Timber.w("L login: username or password not provided")
            loggedIn = false
            return Result.failure(IllegalStateException("не заданы логин и пароль"))
        }

        val formData = mapOf(
            "login" to currentUsername,
            "password" to currentPassword,
            "remember" to "on"
        )

        val (response, body) = try {
            val response = post(LOGIN, formData)
            response to response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w("L login request failed: ${e.javaClass.simpleName}")
            loggedIn = false
            return Result.failure(e)
        }

        val failure = when {
            isCloudflareChallenge(response.headers[CF_MITIGATED_HEADER], body) ->
                IllegalStateException("защита Cloudflare")
            response.status.value in SERVER_ERROR_CODES -> LServerErrorException(response.status.value)
            WRONG_CREDENTIALS_MARKER in body -> LWrongCredentialsException()
            else -> null
        }
        loggedIn = failure == null
        if (failure == null) {
            Timber.i("L login successful")
            return Result.success(Unit)
        }
        Timber.w("L login failed: ${failure.message}")
        return Result.failure(failure)
    }
    // ! --- Login --- !

}

/**
 * Ответ на GraphQL-запрос как есть. Повторить его, счесть отказом сервера или
 * отдать тело решает [Repository][com.client.xvideos.l.repository.Repository].
 *
 * @property statusCode HTTP-код ответа.
 */
class LHttpResponse(
    val statusCode: Int,
    private val body: String,
    private val isCloudflareChallenge: Boolean,
) {
    /**
     * Тело ответа.
     *
     * @throws LServerErrorException сервер ответил 5xx, и это не проверка Cloudflare (та бывает и с кодом 503).
     */
    fun bodyOrServerError(): String {
        if (statusCode in SERVER_ERROR_CODES && !isCloudflareChallenge) throw LServerErrorException(statusCode)
        return body
    }
}

private const val CF_MITIGATED_HEADER = "cf-mitigated"
private const val WRONG_CREDENTIALS_MARKER = "The username and/or password you specified are not correct."
private val SERVER_ERROR_CODES = 500..599

private const val REQUESTED_WITH_HEADER = "X-Requested-With"
private const val REQUESTED_WITH_XHR = "XMLHttpRequest"

/** Предел длины GET-URL: длиннее сервер может ответить 414, такой запрос идёт POST. */
private const val MAX_ANONYMOUS_GET_URL_LENGTH = 8000

private val WHITESPACE_REGEX = Regex("\\s+")
private val MUTATION_REGEX = Regex("^\\s*mutation\\b")
private const val HEX_DIGITS = "0123456789ABCDEF"

/** Символы, которые `encodeURIComponent` оставляет как есть (кроме латиницы и цифр). */
private const val URI_COMPONENT_SAFE = "-_.!~*'()"

/** Символы, которые `URLSearchParams` оставляет как есть (кроме латиницы и цифр). */
private const val FORM_URLENCODED_SAFE = "*-._"

/** Разобранное тело GraphQL-запроса; [variables] — компактный JSON, как `JSON.stringify`. */
private class GraphQlRequest(val operationName: String, val query: String, val variables: String)

private fun parseGraphQlRequest(data: String): GraphQlRequest? {
    val json = runCatching { LJson.parseToJsonElement(data) as? JsonObject }.getOrNull() ?: return null
    val query = (json["query"] as? JsonPrimitive)?.contentOrNull ?: return null
    return GraphQlRequest(
        operationName = (json["operationName"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
        query = query,
        variables = (json["variables"] ?: JsonObject(emptyMap())).toString(),
    )
}

/**
 * URL анонимного GET-запроса GraphQL байт в байт как у сайта
 * (`AnansiFetch.graphql_url_from_params`): параметры `operationName`, `query`,
 * `variables` в этом порядке; пробельные символы запроса схлопнуты в один
 * пробел, текст пропущен через `encodeURIComponent`, затем все параметры —
 * через `URLSearchParams`, поэтому текст запроса закодирован дважды. Ключ
 * кэша Cloudflare — полный URL: расхождение в одном символе означает промах.
 *
 * @return `null` для мутации, неразобранного тела или слишком длинного URL — такой запрос идёт POST.
 */
internal fun anonymousGraphQlGetUrl(endpoint: String, data: String): String? {
    val request = parseGraphQlRequest(data) ?: return null
    if (MUTATION_REGEX.containsMatchIn(request.query)) return null
    val query = WHITESPACE_REGEX.replace(request.query, " ")
    val url = buildString {
        append(endpoint)
        append("?operationName=").append(formUrlEncode(request.operationName))
        append("&query=").append(formUrlEncode(encodeUriComponent(query)))
        append("&variables=").append(formUrlEncode(request.variables))
    }
    return url.takeIf { it.length <= MAX_ANONYMOUS_GET_URL_LENGTH }
}

/** Адрес анонимного POST: как у сайта, имя операции в URL (`fetch_as_post`). */
private fun anonymousGraphQlPostUrl(endpoint: String, data: String): String {
    val operationName = parseGraphQlRequest(data)?.operationName.orEmpty()
    return if (operationName.isEmpty()) endpoint else "$endpoint?operationName=${formUrlEncode(operationName)}"
}

/** JavaScript `encodeURIComponent`. */
private fun encodeUriComponent(value: String): String = percentEncode(value, URI_COMPONENT_SAFE, spaceAsPlus = false)

/** Сериализатор `application/x-www-form-urlencoded` из `URLSearchParams`. */
private fun formUrlEncode(value: String): String = percentEncode(value, FORM_URLENCODED_SAFE, spaceAsPlus = true)

private fun percentEncode(value: String, safe: String, spaceAsPlus: Boolean): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val code = byte.toInt() and 0xFF
        val char = code.toChar()
        when {
            code < 0x80 && (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in safe) -> append(char)
            spaceAsPlus && char == ' ' -> append('+')
            else -> append('%').append(HEX_DIGITS[code shr 4]).append(HEX_DIGITS[code and 0xF])
        }
    }
}

/**
 * Страница проверки Cloudflare («Just a moment...», managed challenge), а не
 * любая страница, прошедшая через Cloudflare.
 *
 * Маркера `challenge-platform` здесь нет намеренно: пассивный скрипт
 * `/cdn-cgi/challenge-platform/scripts/jsd/main.js` Cloudflare вставляет в
 * каждую HTML-страницу, включая страницу 500. По нему отказ сервера L
 * выдавался за блокировку, и вход падал с «blocked by Cloudflare».
 *
 * @param cfMitigated Значение заголовка `cf-mitigated`.
 * @param body Тело ответа.
 */
internal fun isCloudflareChallenge(cfMitigated: String?, body: String): Boolean {
    if (cfMitigated.equals("challenge", ignoreCase = true)) return true
    if (body.isEmpty()) return false
    return body.contains("<title>Just a moment", ignoreCase = true) ||
            body.contains("_cf_chl_opt") ||
            body.contains("cf-chl", ignoreCase = true)
}
