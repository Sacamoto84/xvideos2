package com.client.xvideos.x.feature.net

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.client.xvideos.common.AppContextHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** Сколько ждём `onPageFinished`, прежде чем считать загрузку провалившейся. */
private const val WEB_VIEW_LOAD_TIMEOUT_MS = 45_000L

/**
 * Загружает страницу в headless-WebView и возвращает её HTML после выполнения JS.
 *
 * Возвращает пустую строку, если страница не догрузилась за
 * [WEB_VIEW_LOAD_TIMEOUT_MS] — раньше такого ограничения не было, и если
 * `onPageFinished` не приходил (капча, бесконечный редирект, оборванная сеть),
 * корутина висела вечно, а WebView не уничтожался.
 *
 * Работа с WebView возможна только на main-потоке, поэтому здесь явный
 * `withContext(Dispatchers.Main)` вместо прежнего собственного
 * `CoroutineScope(Dispatchers.Main)`, который не был привязан к вызывающему и
 * продолжал жить после его отмены.
 */
suspend fun readHtmlFromURLWebView(url: String = "https://www.xvideos.com"): String =
    withContext(Dispatchers.Main) {
        withTimeoutOrNull(WEB_VIEW_LOAD_TIMEOUT_MS) {
            loadHtmlInWebView(url)
        } ?: run {
            Timber.w("!!!..readHtmlFromURL timeout $url")
            ""
        }
    }

private suspend fun loadHtmlInWebView(url: String): String =
    suspendCancellableCoroutine { continuation ->

        Timber.i("!!!..readHtmlFromURL $url ")

        val context = AppContextHolder.applicationContext

        val webView = WebView(context)

        // WebView не добавляется в иерархию View, поэтому View.post() мог бы
        // никогда не выполниться. Уничтожаем через main-handler и ровно один раз.
        val mainHandler = Handler(Looper.getMainLooper())
        val destroyed = AtomicBoolean(false)
        fun destroyWebView() {
            if (destroyed.compareAndSet(false, true)) {
                mainHandler.post {
                    runCatching {
                        webView.stopLoading()
                        webView.webViewClient = WebViewClient()
                        webView.destroy()
                    }.onFailure { Timber.w(it, "readHtmlFromURLWebView: destroy failed") }
                }
            }
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                super.onPageFinished(view, finishedUrl)

                if (!continuation.isActive) {
                    destroyWebView()
                    return
                }

                CookieManager.getInstance().flush()

                webView.evaluateJavascript(
                    "(function() { return document.documentElement.outerHTML; })();"
                ) { html ->

                    Timber.i("!!!..readHtmlFromURL end $url")

                    val result = html.trim('"')
                        .replace("\\u003C", "<")
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")

                    if (continuation.isActive) continuation.resume(result)
                    destroyWebView()
                }
            }
        }
        webView.loadUrl(url)
        continuation.invokeOnCancellation { destroyWebView() }
    }
