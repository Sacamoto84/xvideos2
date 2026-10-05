package com.client.xvideos.x.feature.net

import com.client.xvideos.common.net.doh.AppDns
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.util.appendIfNameAbsent
import kotlinx.coroutines.CancellationException
import com.client.xvideos.common.util.pathForLog
import timber.log.Timber
import java.io.IOException

/**
 * Общий клиент для скрапинга страниц.
 *
 * Раньше `HttpClient(OkHttp)` создавался и закрывался на каждый вызов: новый пул
 * соединений и новый пул потоков на каждый запрос, нулевое переиспользование
 * keep-alive. Клиент живёт столько же, сколько процесс, поэтому close() не нужен.
 */
private val htmlClient: HttpClient by lazy {
    HttpClient(OkHttp) {
        engine {
            config {
                dns(AppDns)
            }
        }
        install(HttpTimeout) {
            // Конечные таймауты: зависшее соединение не должно держать корутину/ресурсы вечно.
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        followRedirects = true // Обработка редиректов
        defaultRequest {
            // Только если запрос не задал своё значение: иначе Ktor отправил бы оба через запятую.
            headers.appendIfNameAbsent(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
            headers.appendIfNameAbsent(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
        }
    }
}

/** Ответ сервера с кодом ошибки: адрес не тот или сервер отказал. */
class HttpStatusException(val code: Int, url: String) : IOException("HTTP $code для $url")

/** Ошибка запроса для журнала: код ответа или класс. Текст исключения не годится — в нём адрес запроса. */
internal fun Throwable.logLabel(): String =
    if (this is HttpStatusException) "HTTP $code" else javaClass.simpleName

/**
 * HTML страницы по [url].
 *
 * Ошибку не прячет, в отличие от [readHtmlFromURLDirect]: нет сети или неверный
 * адрес — [IOException], ответ с кодом ошибки — [HttpStatusException]. Так
 * вызывающий отличает «запрос не удался» от «страница пустая».
 *
 * @param extraHeaders Заголовки запроса; одноимённые заголовки клиента по умолчанию они заменяют.
 */
suspend fun fetchHtml(url: String, extraHeaders: Map<String, String> = emptyMap()): String {
    val target = requireHttpUrl(url)
    Timber.d("fetchHtml %s", target.pathForLog())
    val response = htmlClient.get(target) {
        extraHeaders.forEach { (name, value) -> header(name, value) }
    }
    if (!response.status.isSuccess()) throw HttpStatusException(response.status.value, target)
    return response.bodyAsText()
}

/**
 * HTTP POST с form-urlencoded параметрами. Ошибки — как у [fetchHtml].
 *
 * @param url Целевой URL.
 * @param formParameters Словарь параметров формы.
 */
suspend fun postFormData(url: String, formParameters: Map<String, String>): String {
    val target = requireHttpUrl(url)
    val response = htmlClient.post(target) {
        header("Accept", "application/json, text/javascript, */*; q=0.01")
        header("X-Requested-With", "XMLHttpRequest")
        setBody(
            FormDataContent(
                Parameters.build {
                    formParameters.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            )
        )
    }
    if (!response.status.isSuccess()) throw HttpStatusException(response.status.value, target)
    return response.bodyAsText()
}

/**
 * 404 — штатный ответ при переборе адресов (models/channels): «здесь ничего нет»,
 * пустая строка. Остальные ошибки, включая отсутствие сети, летят дальше.
 */
inline fun notFoundAsEmpty(block: () -> String): String = try {
    block()
} catch (e: HttpStatusException) {
    if (e.code == HttpStatusCode.NotFound.value) "" else throw e
}

private fun requireHttpUrl(url: String): String {
    val trimmed = url.trim()
    if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
        throw IOException("Некорректный адрес: $trimmed")
    }
    return trimmed
}

/**
 * Возвращает HTML страницы либо пустую строку при ошибке.
 *
 * Пустая строка как признак ошибки — исторический контракт, на него опираются
 * [SavedX_Downloads], `ScreenTagsViewModel`, плееры X. Где сбой нужно отличать
 * от пустого ответа, вызывать [fetchHtml].
 */
suspend fun readHtmlFromURLDirect(url: String = "https://www.xvideos.com"): String {
    if (url.isBlank()) return ""
    return try {
        fetchHtml(url)
    } catch (e: CancellationException) {
        // Отмену корутины пробрасываем — иначе экран, который уже закрыли,
        // продолжит обрабатывать «успешный» пустой ответ.
        throw e
    } catch (e: Exception) {
        Timber.e("!!! readHtmlFromURLDirect: Ошибка ${e.logLabel()}")
        ""
    }
}

/**
 * HTTP POST с form-urlencoded параметрами; пустая строка при любой ошибке.
 * Где сбой нужно отличать от пустого ответа, вызывать [postFormData].
 *
 * @param url Целевой URL.
 * @param formParameters Словарь параметров формы.
 * @return Ответ сервера в виде строки либо пустая строка при сетевой ошибке.
 */
suspend fun postFormDataFromURLDirect(
    url: String,
    formParameters: Map<String, String>,
): String {
    if (url.isBlank()) return ""
    return try {
        postFormData(url, formParameters)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e("!!! postFormDataFromURLDirect: Ошибка ${e.logLabel()}")
        ""
    }
}
