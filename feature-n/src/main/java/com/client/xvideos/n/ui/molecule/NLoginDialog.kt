package com.client.xvideos.n.ui.molecule

import android.annotation.SuppressLint
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.client.xvideos.n.net.isNTrustedWebUrl
import com.client.xvideos.n.ui.atom.NLoginTopBar
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
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

    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentSuccess by rememberUpdatedState(onSuccess)
    val dismiss = {
        isHandled.set(true)
        currentDismiss()
    }
    val complete: (String, String) -> Unit = { user, session ->
        currentSuccess(user, session)
        currentDismiss()
    }

    LaunchedEffect(webViewInstance) {
        val webView = webViewInstance ?: return@LaunchedEffect
        while (!isHandled.get()) {
            delay(1000L)
            checkNLogin(webView, isHandled, complete)
        }
    }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                NLoginTopBar(
                    onDismiss = dismiss,
                    topInset = topInset
                )

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setupNLoginWebView(this, isHandled, complete)
                            webViewInstance = this
                        }
                    },
                    onRelease = { webView ->
                        isHandled.set(true)
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
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (view != null) checkNLogin(view, isHandled, onLoginDetected)
        }
    }

    webView.loadUrl("https://nhentai.net/login/")
}

private fun checkNLogin(
    webView: WebView,
    isHandled: AtomicBoolean,
    onLoginDetected: (String, String) -> Unit
) {
    val pageUrl = webView.url ?: return
    if (isHandled.get() || !isNTrustedWebUrl(pageUrl)) return
    val cookieManager = CookieManager.getInstance()
    val initialCookies = NCookieHelper.getAllCookies(cookieManager, pageUrl)
    if (NCookieHelper.extractCookieValue(initialCookies, "sessionid").isEmpty()) return

    webView.evaluateJavascript(authenticatedProfileScript) { result ->
        val name = result?.let { runCatching { Json.decodeFromString<String>(it) }.getOrNull() }?.trim().orEmpty()
        if (name.isEmpty() || webView.url != pageUrl || isHandled.get()) return@evaluateJavascript
        val cookies = NCookieHelper.getAllCookies(cookieManager, pageUrl)
        val session = NCookieHelper.extractCookieValue(cookies, "sessionid")
        if (session.isEmpty() || !isHandled.compareAndSet(false, true)) return@evaluateJavascript
        cookieManager.flush()
        NCloudflareState.storeClearance(cookies)
        onLoginDetected(name, session)
    }
}

private val authenticatedProfileScript = """
    (function() {
        if (location.origin !== 'https://nhentai.net') return '';
        var profile = document.querySelector('header a[href*="/users/"], nav a[href*="/users/"], header .username, nav .username');
        var logout = document.querySelector('a[href*="/logout"], form[action*="/logout"]');
        var name = profile && profile.textContent.trim();
        var loginForm = document.querySelector('form input[type="password"]');
        if (!logout && (!name || loginForm || location.pathname.startsWith('/login'))) return '';
        return name || 'Пользователь';
    })()
""".trimIndent()

@Preview
@Composable
private fun NLoginDialogPreview() {
    NLoginDialog(
        onDismiss = {},
        onSuccess = { _, _ -> }
    )
}
