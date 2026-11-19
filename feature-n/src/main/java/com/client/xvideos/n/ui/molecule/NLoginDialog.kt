package com.client.xvideos.n.ui.molecule

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.net.NCookieHelper
import com.client.xvideos.n.net.NUserAgent
import com.client.xvideos.n.ui.atom.NLoginTopBar
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Диалог авторизации в nhentai через встроенный WebView с поддержкой Cloudflare Turnstile.
 * Обнаруживает успешный вход автоматически по cookies и DOM веб-страницы.
 */
@Composable
fun NLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: (username: String, sessionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = getTopInsetDp()
    val isHandled = remember { AtomicBoolean(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(webViewInstance) {
        while (!isHandled.get()) {
            delay(1000L)
            val wv = webViewInstance ?: continue
            val cookieManager = CookieManager.getInstance()
            val allCookies = NCookieHelper.getAllCookies(cookieManager, wv.url)
            val cfClearance = NCookieHelper.extractCookieValue(allCookies, "cf_clearance")
            if (cfClearance.isNotEmpty()) {
                NCloudflareState.onChallengeCompleted(allCookies)
            }
            val sessionId = NCookieHelper.extractCookieValue(allCookies, "sessionid")
            if (sessionId.isNotEmpty() && !isHandled.get()) {
                wv.evaluateJavascript(
                    "(function() { " +
                        "var userLink = document.querySelector('a[href*=\"/users/\"]'); " +
                        "if (userLink) return (userLink.innerText || userLink.textContent || '').trim(); " +
                        "var userSpan = document.querySelector('.username'); " +
                        "if (userSpan) return (userSpan.innerText || userSpan.textContent || '').trim(); " +
                        "return ''; " +
                        "})()"
                ) { result ->
                    if (isHandled.compareAndSet(false, true)) {
                        val nameInDom = result?.trim('"', ' ', '\'').orEmpty().ifEmpty { "Пользователь" }
                        cookieManager.flush()
                        onSuccess(nameInDom, sessionId)
                        onDismiss()
                    }
                }
                break
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                NLoginTopBar(
                    onDismiss = onDismiss,
                    topInset = topInset
                )

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setupNLoginWebView(this, isHandled) { user, session ->
                                onSuccess(user, session)
                                onDismiss()
                            }
                            webViewInstance = this
                        }
                    },
                    onRelease = { webView ->
                        runCatching {
                            webView.stopLoading()
                            webView.destroy()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupNLoginWebView(
    webView: WebView,
    isHandled: AtomicBoolean,
    onLoginDetected: (username: String, sessionId: String) -> Unit
) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)

    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    @Suppress("DEPRECATION")
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        allowFileAccess = false
        allowContentAccess = true
        javaScriptCanOpenWindowsAutomatically = true
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_DEFAULT
        userAgentString = NUserAgent.current
    }

    webView.webChromeClient = WebChromeClient()
    webView.webViewClient = object : WebViewClient() {
        private fun checkAndDetectLogin(view: WebView?, currentUrl: String?) {
            if (isHandled.get()) return

            val allCookies = NCookieHelper.getAllCookies(cookieManager, currentUrl ?: view?.url)
            val cfClearance = NCookieHelper.extractCookieValue(allCookies, "cf_clearance")
            if (cfClearance.isNotEmpty()) {
                NCloudflareState.onChallengeCompleted(allCookies)
            }

            val sessionId = NCookieHelper.extractCookieValue(allCookies, "sessionid")
            if (sessionId.isNotEmpty()) {
                view?.evaluateJavascript(
                    "(function() { " +
                        "var userLink = document.querySelector('a[href*=\"/users/\"]'); " +
                        "if (userLink) return (userLink.innerText || userLink.textContent || '').trim(); " +
                        "var userSpan = document.querySelector('.username'); " +
                        "if (userSpan) return (userSpan.innerText || userSpan.textContent || '').trim(); " +
                        "var logoutLink = document.querySelector('a[href*=\"/logout\"]'); " +
                        "if (logoutLink) return 'Пользователь'; " +
                        "return ''; " +
                        "})()"
                ) { result ->
                    if (isHandled.compareAndSet(false, true)) {
                        val cleanName = result?.trim('"', ' ', '\'').orEmpty().ifEmpty { "Пользователь" }
                        cookieManager.flush()
                        onLoginDetected(cleanName, sessionId)
                    }
                }
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            checkAndDetectLogin(view, url)
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            checkAndDetectLogin(view, url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            checkAndDetectLogin(view, url)
        }
    }

    webView.loadUrl("https://nhentai.net/login/")
}

@Preview
@Composable
private fun NLoginDialogPreview() {
    NLoginDialog(
        onDismiss = {},
        onSuccess = { _, _ -> }
    )
}
