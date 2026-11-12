package com.client.xvideos.n.net

import com.client.xvideos.common.net.UserAgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Сетевой клиент для взаимодействия с сайтом nhentai.net.
 */
object NClient {

    private const val BASE_URL = "https://nhentai.net"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Выполняет GET-запрос к nhentai.net с проверкой статуса Cloudflare.
     */
    suspend fun get(url: String): NNetworkResult = withContext(Dispatchers.IO) {
        val targetUrl = if (url.startsWith("http")) url else "$BASE_URL$url"

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", UserAgentProvider.defaultUserAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
            .header("Referer", BASE_URL)

        NCloudflareState.cfClearanceCookie?.let { clearance ->
            requestBuilder.header("Cookie", "cf_clearance=$clearance")
        }

        try {
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val code = response.code
            val body = response.body.string()

            // Проверка на Cloudflare Challenge
            if (isCloudflareChallenge(code, body)) {
                NCloudflareState.triggerChallenge(targetUrl)
                return@withContext NNetworkResult.CloudflareRequired(targetUrl)
            }

            if (response.isSuccessful) {
                NNetworkResult.Success(body)
            } else {
                NNetworkResult.Error(code, "HTTP error $code")
            }
        } catch (e: IOException) {
            NNetworkResult.Failure(e)
        }
    }

    private fun isCloudflareChallenge(code: Int, body: String): Boolean {
        if (code == 403 || code == 503) return true
        return body.contains("challenges.cloudflare.com") || body.contains("Just a moment...")
    }
}

/**
 * Результат сетевого запроса к nhentai.net.
 */
sealed interface NNetworkResult {
    data class Success(val body: String) : NNetworkResult
    data class CloudflareRequired(val challengeUrl: String) : NNetworkResult
    data class Error(val code: Int, val message: String) : NNetworkResult
    data class Failure(val throwable: Throwable) : NNetworkResult
}
