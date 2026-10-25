package com.client.xvideos.xlive.ui.login.molecule

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.client.xvideos.xlive.auth.SESSION_COOKIE
import com.client.xvideos.xlive.auth.isPastSignInPages
import com.client.xvideos.xlive.auth.signInStep
import com.client.xvideos.xlive.auth.xLiveCookie
import com.client.xvideos.xlive.auth.xLiveWebViewCookies
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.room.atom.XLiveMessage
import timber.log.Timber

private const val FULL_PROGRESS = 100

/**
 * Страница входа сайта в WebView: логин, пароль, капчу и второй фактор
 * пользователь вводит сам, приложение их не видит. Когда загрузилась
 * страница сайта вне служебных страниц входа и cookie сессии есть,
 * вызывается [onLeftSignInPages] — на каждой такой странице: подтвердит ли
 * сайт вход, решает экран.
 *
 * WebView живёт, пока страница в композиции: Activity не пересоздаётся ни при
 * повороте, ни при сложении складного (их она обрабатывает сама).
 *
 * @param url страница входа.
 */
@Composable
fun XLiveLoginPage(
    url: String,
    onLeftSignInPages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnLeft by rememberUpdatedState(onLeftSignInPages)
    var progress by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                createLoginWebView(
                    context = context,
                    onProgress = { progress = it },
                    onFailed = { failed = true },
                    onLeftSignInPages = { currentOnLeft() },
                ).also {
                    webView = it
                    it.loadUrl(url)
                }
            },
            onRelease = { view ->
                webView = null
                runCatching {
                    view.stopLoading()
                    view.destroy()
                }.onFailure { Timber.w("XLive: WebView входа не уничтожен: ${it.javaClass.simpleName}") }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (progress in 1 until FULL_PROGRESS) {
            LinearProgressIndicator(
                progress = { progress / FULL_PROGRESS.toFloat() },
                color = XLiveColors.Accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
        if (failed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(XLiveColors.Background),
                contentAlignment = Alignment.Center,
            ) {
                XLiveMessage(
                    text = "Страница входа не загрузилась",
                    onRetry = {
                        failed = false
                        webView?.loadUrl(url)
                    },
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createLoginWebView(
    context: Context,
    onProgress: (Int) -> Unit,
    onFailed: () -> Unit,
    onLeftSignInPages: () -> Unit,
): WebView {
    // Логин и пароль пользователь вводит сам, в форму сайта; автозаполнение —
    // его менеджера паролей, как в браузере. Приложение пароль не видит.
    val webView = WebView(context)
    xLiveWebViewCookies()?.let { manager ->
        manager.setAcceptCookie(true)
        manager.setAcceptThirdPartyCookies(webView, false)
    }
    // JavaScript включён намеренно: форму и капчу ведут скрипты страницы.
    // Моста в приложение нет — ни одного @JavascriptInterface.
    with(webView.settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    }
    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            onProgress(newProgress)
        }

        // Консоль страницы в журнал не идёт: WebView печатает её вместе с адресом.
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean = true
    }
    webView.webViewClient = LoginPageClient(onFailed, onLeftSignInPages)
    return webView
}

/** Следит за главным фреймом: ошибка загрузки и уход со страниц входа с сессией. */
private class LoginPageClient(
    private val onFailed: () -> Unit,
    private val onLeftSignInPages: () -> Unit,
) : WebViewClient() {

    // Ссылки в другие приложения (`intent:`, `mailto:`) не открываются: окно — только для входа.
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val scheme = request?.url?.scheme
        return scheme != "https" && scheme != "http"
    }

    // Страницу не обрываем: после формы сайт может вести по своим шагам, и
    // каждый должен догрузиться.
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url == null) return
        Timber.i("XLive: вход — страница %s", signInStep(url))
        if (isPastSignInPages(url, hasSession = xLiveCookie(SESSION_COOKIE) != null)) {
            xLiveWebViewCookies()?.flush()
            onLeftSignInPages()
        }
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            Timber.w("XLive: страница входа не загрузилась: ${error?.errorCode}")
            onFailed()
        }
    }
}

@Preview
@Composable
private fun XLiveLoginPagePreview() {
    XLiveLoginPage(url = "about:blank", onLeftSignInPages = {})
}
