package com.client.xvideos.n.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Сетевой клиент для взаимодействия с сайтом nhentai.net.
 */
object NClient {

    private const val BASE_URL = "https://nhentai.net"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

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
            .header("User-Agent", NUserAgent.current)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
            .header("Referer", BASE_URL)

        val apiKey = NAuthState.currentApiKey
        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Key $apiKey")
        }

        buildCookieHeader()?.let { cookieHeader ->
            requestBuilder.header("Cookie", cookieHeader)
        }

        executeRequest(requestBuilder.build(), targetUrl)
    }

    /**
     * Выполняет POST-запрос с JSON телом к nhentai.net.
     */
    suspend fun postJson(url: String, jsonBody: String): NNetworkResult = withContext(Dispatchers.IO) {
        val targetUrl = if (url.startsWith("http")) url else "$BASE_URL$url"
        val requestBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .post(requestBody)
            .header("User-Agent", NUserAgent.current)
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
            .header("Referer", BASE_URL)

        val apiKey = NAuthState.currentApiKey
        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Key $apiKey")
        }

        buildCookieHeader()?.let { cookieHeader ->
            requestBuilder.header("Cookie", cookieHeader)
        }

        executeRequest(requestBuilder.build(), targetUrl)
    }

    private fun executeRequest(request: Request, targetUrl: String): NNetworkResult {
        return try {
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val body = response.body.string()

            if (isCloudflareChallenge(code, body)) {
                val challengeUrl = if (targetUrl.contains("/api/")) BASE_URL else targetUrl
                NCloudflareState.triggerChallenge(challengeUrl)
                return NNetworkResult.CloudflareRequired(challengeUrl)
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

    private val cfMarkers = listOf(
        "challenges.cloudflare.com",
        "Just a moment...",
        "cf-turnstile",
        "window._cf_chl_opt"
    )

    private fun isCloudflareChallenge(code: Int, body: String): Boolean {
        if (body.contains("Use new API", ignoreCase = true)) return false
        if (cfMarkers.any { body.contains(it) }) return true
        val isBlockCode = code == 403 || code == 503
        if (!isBlockCode) return false
        return body.contains("<html", ignoreCase = true) && body.contains("Cloudflare", ignoreCase = true)
    }

    private fun buildCookieHeader(): String? {
        val cookies = mutableListOf<String>()
        NCloudflareState.cfClearanceCookie?.let { clearance ->
            cookies.add("cf_clearance=$clearance")
        }
        val sessionCookie = NAuthState.currentSessionCookie
        if (sessionCookie.isNotBlank()) {
            cookies.add("sessionid=$sessionCookie")
        }
        return if (cookies.isNotEmpty()) cookies.joinToString("; ") else null
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
