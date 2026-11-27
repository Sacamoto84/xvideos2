package com.client.xvideos.n.ui.molecule

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.net.NCookieHelper
import com.client.xvideos.n.net.NUserAgent
import com.client.xvideos.n.net.isNTrustedWebUrl
import com.client.xvideos.n.ui.atom.NLoginTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                NLoginTopBar(onDismiss = onDismiss, topInset = topInset)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("WebView входа N", color = Color.LightGray)
                }
            }
        }
        return
    }

    val isHandled = remember { AtomicBoolean(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val ioScope = rememberCoroutineScope()

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
    val flushCookiesAsync = {
        ioScope.launch(Dispatchers.IO) { runCatching { CookieManager.getInstance().flush() } }
        Unit
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        webViewInstance?.onResume()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        webViewInstance?.onPause()
    }

    LaunchedEffect(webViewInstance) {
        val webView = webViewInstance ?: return@LaunchedEffect
        while (!isHandled.get()) {
            delay(1000L)
            if (!isHandled.get()) {
                checkNLogin(webView, isHandled, flushCookiesAsync, complete)
            }
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
                            setupNLoginWebView(this, isHandled, flushCookiesAsync, complete)
                            webViewInstance = this
                        }
                    },
                    onRelease = { webView ->
                        isHandled.set(true)
                        webViewInstance = null
                        runCatching {
                            webView.stopLoading()
                            webView.webChromeClient = WebChromeClient()
                            webView.webViewClient = WebViewClient()
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
    flushCookiesAsync: () -> Unit,
    onLoginDetected: (username: String, sessionId: String) -> Unit
) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)

    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        javaScriptCanOpenWindowsAutomatically = true
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_DEFAULT
        userAgentString = NUserAgent.current
    }

    webView.webChromeClient = WebChromeClient()
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return !isNAllowedNavigationUri(request?.url, request?.isForMainFrame == true)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (view != null) checkNLogin(view, isHandled, flushCookiesAsync, onLoginDetected)
        }
    }

    webView.loadUrl("https://nhentai.net/login/")
}

private fun checkNLogin(
    webView: WebView,
    isHandled: AtomicBoolean,
    flushCookiesAsync: () -> Unit,
    onLoginDetected: (String, String) -> Unit
) {
    if (isHandled.get()) return
    val pageUrl = webView.url ?: return
    if (isHandled.get() || !isNTrustedWebUrl(pageUrl)) return
    val cookieManager = CookieManager.getInstance()
    val initialCookies = NCookieHelper.getAllCookies(cookieManager, pageUrl)
    if (NCookieHelper.extractCookieValue(initialCookies, "sessionid").isEmpty()) return

    webView.evaluateJavascript(authenticatedProfileScript) { result ->
        if (isHandled.get()) return@evaluateJavascript
        val name = result?.let { runCatching { Json.decodeFromString<String>(it) }.getOrNull() }?.trim().orEmpty()
        if (name.isEmpty() || webView.url != pageUrl || isHandled.get()) return@evaluateJavascript
        val cookies = NCookieHelper.getAllCookies(cookieManager, pageUrl)
        val session = NCookieHelper.extractCookieValue(cookies, "sessionid")
        if (session.isEmpty() || !isHandled.compareAndSet(false, true)) return@evaluateJavascript
        flushCookiesAsync()
        NCloudflareState.storeClearance(cookies)
        onLoginDetected(name, session)
    }
}

internal fun isNAllowedNavigationUri(uri: Uri?, isForMainFrame: Boolean): Boolean {
    if (uri == null) return false
    if (!uri.scheme.equals("https", ignoreCase = true)) return false
    if (!isForMainFrame) return true
    val host = uri.host?.lowercase().orEmpty()
    return host == "nhentai.net" ||
        host.endsWith(".nhentai.net") ||
        host == "challenges.cloudflare.com" ||
        host.endsWith(".hcaptcha.com")
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
