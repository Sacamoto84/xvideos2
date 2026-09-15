package com.client.xvideos.x.search

import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.x.urlStart
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.net.URLEncoder

private val searchHttpClient: HttpClient by lazy {
    HttpClient(OkHttp) {
        engine {
            config {
                dns(AppDns)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }

        defaultRequest {
            headers.append("Referer", "$urlStart/")
            headers.append("Origin", urlStart)
            headers.append(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 YaBrowser/25.6.0.0 Safari/537.36"
            )
            headers.append(
                HttpHeaders.Accept,
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            )
            headers.append(HttpHeaders.AcceptEncoding, "identity")
            headers.append(HttpHeaders.AcceptLanguage, "ru,en;q=0.9")
        }
    }
}

@Deprecated("Не используется в проекте; оставлен для возможной интеграции search-suggest")
suspend fun getSearchResults(query: String): String? {
    if (query.isBlank()) return null
    // Кодируем пользовательский ввод: пробелы/спецсимволы не должны ломать URL.
    val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
    val url = "$urlStart/search-suggest/$encodedQuery"

    return try {
        val response = searchHttpClient.get(url)
        if (!response.status.isSuccess()) {
            Timber.w("getSearchResults: HTTP ${response.status.value} for $url")
            null
        } else {
            response.bodyAsText()
        }
    } catch (e: CancellationException) {
        // Иначе отмена возвращалась как null и трактовалась как «ничего не найдено».
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Ошибка getSearchResults: ${e.message}")
        null
    }
}
