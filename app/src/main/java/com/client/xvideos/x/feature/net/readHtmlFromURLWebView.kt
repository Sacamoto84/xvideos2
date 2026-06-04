package com.client.xvideos.x.feature.net

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.client.xvideos.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

suspend fun readHtmlFromURLWebView(url: String = "https://www.xvideos.com"): String =

    suspendCancellableCoroutine { continuation ->

        CoroutineScope(Dispatchers.Main).launch {

            Timber.i("!!!..readHtmlFromURL $url ")

            val context = App.instance.applicationContext

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
    }
