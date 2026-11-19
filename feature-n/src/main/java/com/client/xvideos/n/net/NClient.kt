package com.client.xvideos.n.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import android.webkit.CookieManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Сетевой клиент для взаимодействия с сайтом nhentai.net.
 */
object NClient {

    private const val BASE_URL = "https://nhentai.net"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val rateLimit = NRateLimit()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(false)
        .addNetworkInterceptor { chain -> chain.proceed(requireTrustedNRequest(chain.request())) }
        .build()

    /**
     * Выполняет GET-запрос к nhentai.net с проверкой статуса Cloudflare.
     */
    suspend fun get(url: String): NNetworkResult = withContext(Dispatchers.IO) {
        val targetUrl = if (url.startsWith("http")) url else "$BASE_URL$url"
        if (!isNTrustedWebUrl(targetUrl)) return@withContext NNetworkResult.Failure(untrustedNOrigin())

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
        if (!isNTrustedWebUrl(targetUrl)) return@withContext NNetworkResult.Failure(untrustedNOrigin())
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

    suspend fun setFavorite(galleryId: Long, favorite: Boolean, apiKey: String): NNetworkResult = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/api/v2/galleries/$galleryId/favorite"
        val request = Request.Builder().url(url)
            .header("User-Agent", NUserAgent.current)
            .header("Accept", "application/json")
            .header("Authorization", "Key $apiKey")
        buildCookieHeader()?.let { request.header("Cookie", it) }
        if (favorite) request.post("".toRequestBody(null)) else request.delete()
        executeRequest(request.build(), url)
    }

    private suspend fun executeRequest(request: Request, targetUrl: String): NNetworkResult {
        rateLimit.blockedRequest()?.let { return it }
        return suspendCancellableCoroutine { continuation ->
            val call = okHttpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(NNetworkResult.Failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = try {
                        response.use {
                            val challengeUrl = if (request.url.encodedPath.startsWith("/api/")) BASE_URL else targetUrl
                            it.toNNetworkResult(challengeUrl, rateLimit)
                        }
                    } catch (e: IOException) {
                        NNetworkResult.Failure(e)
                    }
                    continuation.resume(result)
                }
            })
        }
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
        runCatching {
            val cmCookies = CookieManager.getInstance().getCookie(BASE_URL).orEmpty()
            val csrf = NCookieHelper.extractCookieValue(cmCookies, "csrftoken")
            if (csrf.isNotBlank() && cookies.none { it.startsWith("csrftoken=") }) {
                cookies.add("csrftoken=$csrf")
            }
        }
        return if (cookies.isNotEmpty()) cookies.joinToString("; ") else null
    }
}

internal fun Response.toNNetworkResult(challengeUrl: String, rateLimit: NRateLimit): NNetworkResult {
    val text = if (isSuccessful || code == 403 || code == 503) body.string() else ""
    return when {
        isNCloudflareChallenge(code, header("Content-Type"), header("cf-mitigated"), text) ->
            NNetworkResult.CloudflareRequired(
                challengeUrl,
                NCookieHelper.extractCookieValue(request.header("Cookie").orEmpty(), "cf_clearance")
            )
        code == 429 -> rateLimit.onRateLimited(headers)
        isSuccessful -> NNetworkResult.Success(text, request.url.toString())
        else -> NNetworkResult.Error(code, "HTTP error $code")
    }
}

internal fun requireTrustedNRequest(request: Request): Request {
    if (!isNTrustedWebUrl(request.url.toString())) throw untrustedNOrigin()
    return request
}

private fun untrustedNOrigin() = IOException("Запрос N отклонён: адрес вне доверенного HTTPS-источника")

internal fun isNCloudflareChallenge(code: Int, contentType: String?, mitigation: String?, body: String): Boolean {
    if (mitigation.equals("challenge", ignoreCase = true)) return true
    if (code != 403 && code != 503) return false
    if (contentType != null && !contentType.startsWith("text/html", ignoreCase = true)) return false
    val html = body.trimStart()
    if (!html.startsWith("<!doctype html", ignoreCase = true) && !html.startsWith("<html", ignoreCase = true)) return false
    return html.contains("window._cf_chl_opt") ||
        html.contains("/cdn-cgi/challenge-platform/") ||
        html.contains("<title>Just a moment...</title>", ignoreCase = true) ||
        (html.contains("Cloudflare", ignoreCase = true) && html.contains("cf-turnstile"))
}

/**
 * Результат сетевого запроса к nhentai.net.
 */
sealed interface NNetworkResult {
    data class Success(val body: String, val finalUrl: String) : NNetworkResult
    class CloudflareRequired(val challengeUrl: String, val sentClearance: String) : NNetworkResult
    data class Error(val code: Int, val message: String) : NNetworkResult
    data class Failure(val throwable: Throwable) : NNetworkResult
}
