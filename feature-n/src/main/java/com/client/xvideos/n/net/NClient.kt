package com.client.xvideos.n.net

import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.common.settings.element.NAccessCredentials

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
        .dns(AppDns)
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

        requestBuilder.addCredentials(NAuthState.currentCredentials)?.let { return@withContext it }
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

        requestBuilder.addCredentials(NAuthState.currentCredentials)?.let { return@withContext it }
        executeRequest(requestBuilder.build(), targetUrl)
    }

    suspend fun setFavorite(galleryId: Long, favorite: Boolean, apiKey: String): NNetworkResult = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/api/v2/galleries/$galleryId/favorite"
        val request = Request.Builder().url(url)
            .header("User-Agent", NUserAgent.current)
            .header("Accept", "application/json")
        request.addCredentials(NAccessCredentials(apiKey = apiKey))?.let { return@withContext it }
        if (favorite) request.post("".toRequestBody(null)) else request.delete()
        executeRequest(request.build(), url)
    }

    private fun Request.Builder.addCredentials(credentials: NAccessCredentials): NNetworkResult.Failure? {
        val apiKey = credentials.apiKey
        if (apiKey.isNotBlank()) {
            if (apiKey.any { it !in '!'..'~' }) {
                return NNetworkResult.Failure(IllegalArgumentException("Проверьте формат API-ключа N в настройках"))
            }
            header("Authorization", "Key $apiKey")
        }
        val webCookies = runCatching { CookieManager.getInstance().getCookie(BASE_URL).orEmpty() }.getOrDefault("")
        nCookieHeader(credentials.sessionCookie, NCloudflareState.cfClearanceCookie, webCookies)?.let {
            header("Cookie", it)
        }
        return null
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

}

internal fun nCookieHeader(sessionCookie: String, cachedClearance: String?, webCookies: String): String? {
    val clearance = NCookieHelper.extractCookieValue(webCookies, "cf_clearance").takeIf(::isNCookieValue)
        ?: cachedClearance?.takeIf(::isNCookieValue)
    val csrf = NCookieHelper.extractCookieValue(webCookies, "csrftoken")
    val cookies = buildList {
        if (clearance != null) add("cf_clearance=$clearance")
        if (isNCookieValue(sessionCookie)) add("sessionid=$sessionCookie")
        if (isNCookieValue(csrf)) add("csrftoken=$csrf")
    }
    return cookies.takeIf { it.isNotEmpty() }?.joinToString("; ")
}

private fun isNCookieValue(value: String): Boolean =
    value.isNotEmpty() && value.all { it in '!'..'~' && it != ';' }

internal fun Response.toNNetworkResult(challengeUrl: String, rateLimit: NRateLimit): NNetworkResult {
    val mitigation = header("cf-mitigated")
    if (mitigation.equals("challenge", ignoreCase = true)) return cloudflareRequired(challengeUrl)
    if (code == 429) return rateLimit.onRateLimited(headers)
    if (isSuccessful) return NNetworkResult.Success(body.string(), request.url.toString())

    val contentType = header("Content-Type")
    return when {
        isNHtmlBlockingResponse(code, contentType) && isNCloudflareChallenge(code, contentType, mitigation, body.string()) ->
            cloudflareRequired(challengeUrl)
        else -> NNetworkResult.Error(code, "HTTP error $code")
    }
}

private fun Response.cloudflareRequired(challengeUrl: String) = NNetworkResult.CloudflareRequired(
    challengeUrl,
    NCookieHelper.extractCookieValue(request.header("Cookie").orEmpty(), "cf_clearance")
)

internal fun requireTrustedNRequest(request: Request): Request {
    if (!isNTrustedWebUrl(request.url.toString())) throw untrustedNOrigin()
    return request
}

private fun untrustedNOrigin() = IOException("Запрос N отклонён: адрес вне доверенного HTTPS-источника")

internal fun isNCloudflareChallenge(code: Int, contentType: String?, mitigation: String?, body: String): Boolean {
    if (mitigation.equals("challenge", ignoreCase = true)) return true
    if (!isNHtmlBlockingResponse(code, contentType)) return false
    val html = body.trimStart()
    if (!html.startsWith("<!doctype html", ignoreCase = true) && !html.startsWith("<html", ignoreCase = true)) return false
    return html.contains("window._cf_chl_opt") ||
        html.contains("/cdn-cgi/challenge-platform/") ||
        html.contains("<title>Just a moment...</title>", ignoreCase = true) ||
        (html.contains("Cloudflare", ignoreCase = true) && html.contains("cf-turnstile"))
}

private fun isNHtmlBlockingResponse(code: Int, contentType: String?): Boolean =
    (code == 403 || code == 503) && (contentType == null || contentType.startsWith("text/html", ignoreCase = true))

/**
 * Результат сетевого запроса к nhentai.net.
 */
sealed interface NNetworkResult {
    data class Success(val body: String, val finalUrl: String) : NNetworkResult
    class CloudflareRequired(val challengeUrl: String, val sentClearance: String) : NNetworkResult
    data class Error(val code: Int, val message: String) : NNetworkResult
    data class Failure(val throwable: Throwable) : NNetworkResult
}
