package com.client.xvideos.x.feature.net

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.client.xvideos.common.AppContextHolder
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.builtins.serializer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.client.xvideos.common.util.pathForLog
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
suspend fun readHtmlFromURLWebView(url: String = "https://www.xvideos.com"): String {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return ""
    if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
        Timber.w("readHtmlFromURLWebView: invalid scheme, url length ${trimmed.length}")
        return ""
    }
    return withContext(Dispatchers.Main) {
        withTimeoutOrNull(WEB_VIEW_LOAD_TIMEOUT_MS) {
            loadHtmlInWebView(trimmed)
        } ?: run {
            Timber.w("!!!..readHtmlFromURL timeout ${trimmed.pathForLog()}")
            ""
        }
    }
}

private suspend fun loadHtmlInWebView(url: String): String =
    suspendCancellableCoroutine { continuation ->

        Timber.d("readHtmlFromURL %s", url.pathForLog())

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
        // Сторонние cookie разбору страницы не нужны: страну и сессию сайт держит в своих.
        cookieManager.setAcceptThirdPartyCookies(webView, false)

        // JavaScript включён намеренно и отключить его нельзя: смысл этого
        // WebView — получить HTML *после* выполнения скриптов страницы, обычным
        // GET такую разметку не добыть. Моста в приложение при этом нет: ни
        // одного @JavascriptInterface здесь не регистрируется, так что скриптам
        // страницы некуда выйти за пределы самого WebView.
        @SuppressLint("SetJavaScriptEnabled")
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            // Закрывает странице доступ к content://-провайдерам. По умолчанию
            // это разрешено, а у приложения есть FileProvider — пусть даже
            // неэкспортированный, запас лишним не будет.
            allowContentAccess = false
            // На targetSdk 21+ это и так значение по умолчанию; ставим явно,
            // чтобы смена умолчания в новой версии не прошла незамеченной.
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            // Нужна только разметка: адреса превью парсер берёт из атрибутов, а сами
            // картинки потом грузит Coil. С картинками WebView качал их все вторым
            // потоком, и onPageFinished ждал их.
            loadsImagesAutomatically = false
            blockNetworkImage = true
        }

        // Консоль страницы в журнал не идёт: по умолчанию WebView печатает её
        // в logcat вместе с адресом страницы.
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean = true
        }

        webView.webViewClient = object : WebViewClient() {

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Timber.w("readHtmlFromURLWebView: onReceivedError $errorCode: $description for ${failingUrl?.pathForLog()}")
                if (continuation.isActive) continuation.resume("")
                destroyWebView()
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    Timber.w("readHtmlFromURLWebView: main frame error for ${request.url.path}")
                    if (continuation.isActive) continuation.resume("")
                    destroyWebView()
                }
            }

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

                    Timber.d("readHtmlFromURL end %s", url.pathForLog())

                    val result = decodeJsStringResult(html)
                    if (continuation.isActive) continuation.resume(result)
                    destroyWebView()
                }
            }
        }
        webView.loadUrl(url)
        continuation.invokeOnCancellation { destroyWebView() }
    }

/**
 * Результат `evaluateJavascript` — JSON-литерал строки. Разбираем его парсером
 * JSON: ручные замены пропускали `\\`, `\t`, `\r`, `\/` и прочие `\uXXXX`, и
 * ссылки и данные из скриптов страницы приходили испорченными.
 *
 * @return HTML страницы; пустая строка, если скрипт ничего не вернул или ответ не разобрался.
 */
internal fun decodeJsStringResult(raw: String?): String {
    if (raw.isNullOrEmpty() || raw == "null") return ""
    return runCatching { Json.decodeFromString(String.serializer(), raw) }
        .getOrElse { e ->
            Timber.w("readHtmlFromURLWebView: результат JS не разобран как строка JSON: ${e.javaClass.simpleName}")
            ""
        }
}
