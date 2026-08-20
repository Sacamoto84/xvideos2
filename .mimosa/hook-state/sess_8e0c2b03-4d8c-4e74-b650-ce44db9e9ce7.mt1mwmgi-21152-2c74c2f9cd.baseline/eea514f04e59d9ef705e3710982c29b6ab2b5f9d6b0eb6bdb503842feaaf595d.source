package com.client.xvideos.x.feature.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Общий клиент для скрапинга страниц.
 *
 * Раньше `HttpClient(OkHttp)` создавался и закрывался на каждый вызов: новый пул
 * соединений и новый пул потоков на каждый запрос, нулевое переиспользование
 * keep-alive. Клиент живёт столько же, сколько процесс, поэтому close() не нужен.
 */
private val htmlClient: HttpClient by lazy {
    HttpClient(OkHttp) {
        install(HttpTimeout) {
            // Конечные таймауты: зависшее соединение не должно держать корутину/ресурсы вечно.
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        followRedirects = true // Обработка редиректов
        defaultRequest {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
            header("Accept-Language", "en-US,en;q=0.9")
        }
    }
}

/**
 * Возвращает HTML страницы либо пустую строку при ошибке.
 *
 * Пустая строка как признак ошибки — исторический контракт, на него опираются
 * все вызывающие ([SavedX_Downloads], `ScreenTagsViewModel`, плееры X).
 */
suspend fun readHtmlFromURLDirect(url: String = "https://www.xvideos.com"): String {

    Timber.i("!!!..readHtmlFromURLDirect $url ")

    return try {
        htmlClient.get(url).bodyAsText()
    } catch (e: CancellationException) {
        // Отмену корутины пробрасываем — иначе экран, который уже закрыли,
        // продолжит обрабатывать «успешный» пустой ответ.
        throw e
    } catch (e: Exception) {
        Timber.e(e, "!!! readHtmlFromURLDirect: Ошибка ${e.message}")
        ""
    }
}
