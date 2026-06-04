package com.client.xvideos.x.screens.search

import com.client.xvideos.urlStart
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import timber.log.Timber
import java.net.URLEncoder

suspend fun getSearchResults(query: String): String? {

    val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            // Конечные таймауты вместо Long.MAX_VALUE.
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }

        defaultRequest {
            // Referer/Origin относятся к самому сайту, а не к стороннему redgifs.
            headers.append("Referer", "$urlStart/")
            headers.append("Origin", urlStart)
            headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 YaBrowser/25.6.0.0 Safari/537.36")
            headers.append(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            headers.append(HttpHeaders.AcceptEncoding, "identity")
            headers.append(HttpHeaders.AcceptLanguage, "ru,en;q=0.9")
        }
    }

    // Кодируем пользовательский ввод: пробелы/спецсимволы не должны ломать URL.
    val encodedQuery = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
    val url = "$urlStart/search-suggest/$encodedQuery"

    return try {
        client.get(url).bodyAsText()
    } catch (e: Exception) {
        Timber.e("Ошибка " + e.message)
        null
    } finally {
        client.close() // раньше клиент не закрывался → утечка пула соединений/потоков
    }
}
