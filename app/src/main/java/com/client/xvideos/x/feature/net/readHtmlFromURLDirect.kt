package com.client.xvideos.x.feature.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import timber.log.Timber

suspend fun readHtmlFromURLDirect(url: String = "https://www.xvideos.com"): String {

    Timber.i("!!!..readHtmlFromURLDirect $url ")

    val client = HttpClient(OkHttp)
    {
        install(HttpTimeout)
        {
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

    try {
        val body = client.get(url).bodyAsText()
        return body
    } catch (e: Exception) {
        Timber.e("!!! readHtmlFromURLDirect: Ошибка %s", e.message)
    } finally {
        client.close()
    }
    return ""
}