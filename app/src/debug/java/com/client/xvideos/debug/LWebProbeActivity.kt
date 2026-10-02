package com.client.xvideos.debug

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.l.repository.LusciousEndpoints
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Эксперимент, только debug-сборка: получает ли приложение данные L через
 * сайт в WebView, пока сервер отвечает 500 на всё, чего нет в кэше Cloudflare.
 *
 * WebView открывает сайт, и его собственный JS шлёт анонимные GET-запросы к
 * GraphQL — ровно с теми текстами и переменными, что в браузере. Каждый такой
 * запрос перехватывается в [WebViewClient.shouldInterceptRequest] и
 * выполняется через OkHttp тем же URL (ключ кэша Cloudflare совпадает), в
 * logcat с тегом [TAG] пишутся операция, переменные, статус, `cf-cache-status`
 * и число альбомов, а ответ отдаётся странице. Сторонние хосты (реклама,
 * счётчики) режутся пустым ответом, уход со страницы на чужой домен запрещён.
 *
 * Запуск: `adb shell am start -n com.client.xvideos/.debug.LWebProbeActivity`,
 * другая страница сайта — `--es path /albums/new/`.
 */
class LWebProbeActivity : Activity() {

    private val client = OkHttpClient.Builder()
        .dns(AppDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val siteUri = Uri.parse(LusciousEndpoints.API_ANONYMOUS)
    private val siteDomain = siteUri.host.orEmpty().removePrefix("www.")
    private val blocked = AtomicInteger()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(EXTRA_PATH) ?: "/"
        val webView = WebView(this)
        setContentView(webView)
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
        }
        webView.webViewClient = ProbeClient()
        Timber.tag(TAG).i("start path=%s", path)
        webView.loadUrl("${siteUri.scheme}://${siteUri.host}$path")
    }

    private inner class ProbeClient : WebViewClient() {

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val host = request.url.host.orEmpty()
            if (!isAllowedHost(host, siteDomain)) {
                blocked.incrementAndGet()
                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
            }
            val description = describeGraphQlRequest(request.url.toString()) ?: return null
            if (request.method != "GET") {
                Timber.tag(TAG).i("%s method=%s пропущен без перехвата", description, request.method)
                return null
            }
            return fetchAndLog(request, description)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            !isAllowedHost(request.url.host.orEmpty(), siteDomain)

        override fun onPageFinished(view: WebView, url: String) {
            Timber.tag(TAG).i("page finished path=%s blocked=%d", Uri.parse(url).path, blocked.get())
        }
    }

    /** Выполняет запрос страницы тем же URL и заголовками (без cookies) и пишет итог в лог. */
    private fun fetchAndLog(request: WebResourceRequest, description: String): WebResourceResponse? {
        val okRequest = Request.Builder()
            .url(request.url.toString())
            .apply {
                request.requestHeaders
                    .filterKeys { !it.equals("Cookie", ignoreCase = true) }
                    .forEach { (name, value) -> header(name, value) }
            }
            .build()
        return try {
            client.newCall(okRequest).execute().use { response ->
                val body = response.body?.bytes() ?: ByteArray(0)
                Timber.tag(TAG).i(
                    "%s -> %d cache=%s bytes=%d albums=%d",
                    description,
                    response.code,
                    response.header("cf-cache-status"),
                    body.size,
                    countAlbums(body.toString(Charsets.UTF_8)),
                )
                WebResourceResponse(
                    "application/json",
                    "utf-8",
                    response.code,
                    response.message.ifBlank { if (response.isSuccessful) "OK" else "Error" },
                    mapOf("Content-Type" to (response.header("Content-Type") ?: "application/json")),
                    ByteArrayInputStream(body),
                )
            }
        } catch (e: IOException) {
            Timber.tag(TAG).w("%s -> %s: %s", description, e.javaClass.simpleName, e.message)
            null
        }
    }

    private companion object {
        const val TAG = "LWebProbe"
        const val EXTRA_PATH = "path"
    }
}
