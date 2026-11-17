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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.net.NAuthState
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.net.NCookieHelper
import com.client.xvideos.n.net.NUserAgent
import com.client.xvideos.n.ui.atom.NLoginManualInput
import com.client.xvideos.n.ui.atom.NLoginTopBar
import kotlinx.coroutines.delay

/**
 * Диалог авторизации в nhentai через встроенный WebView с поддержкой Cloudflare Turnstile
 * или ручной ввод API-ключа / sessionid.
 */
@Composable
fun NLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: (username: String, sessionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = getTopInsetDp()
    var isManualMode by remember { mutableStateOf(false) }
    var manualToken by remember { mutableStateOf("") }
    var manualUsername by remember { mutableStateOf("") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    fun verifyAndCompleteManualCheck() {
        val wv = webViewInstance ?: return
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()
        val allCookies = NCookieHelper.getAllCookies(cookieManager, wv.url)
        val cf = NCookieHelper.extractCookieValue(allCookies, "cf_clearance")
        if (cf.isNotEmpty()) {
            NCloudflareState.onChallengeCompleted(allCookies)
        }
        val sessionId = NCookieHelper.extractCookieValue(allCookies, "sessionid")

        wv.evaluateJavascript(
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
            val nameInDom = result?.trim('"', ' ', '\'').orEmpty()
            if (sessionId.isNotEmpty() || nameInDom.isNotEmpty()) {
                val cleanUser = nameInDom.ifEmpty { "Пользователь" }
                val token = sessionId.ifEmpty {
                    NCookieHelper.extractCookieValue(allCookies, "csrftoken").ifEmpty { "session" }
                }
                cookieManager.flush()
                onSuccess(cleanUser, token)
                onDismiss()
            } else {
                SnackBar.error("Сессия не найдена. Убедитесь, что вошли в аккаунт на сайте.")
            }
        }
    }

    LaunchedEffect(isManualMode, webViewInstance) {
        if (!isManualMode) {
            while (true) {
                delay(1500L)
                val wv = webViewInstance ?: continue
                val cookieManager = CookieManager.getInstance()
                val allCookies = NCookieHelper.getAllCookies(cookieManager, wv.url)
                val sessionId = NCookieHelper.extractCookieValue(allCookies, "sessionid")
                if (sessionId.isNotEmpty()) {
                    wv.evaluateJavascript(
                        "(function() { " +
                            "var userLink = document.querySelector('a[href*=\"/users/\"]'); " +
                            "if (userLink) return (userLink.innerText || userLink.textContent || '').trim(); " +
                            "var userSpan = document.querySelector('.username'); " +
                            "if (userSpan) return (userSpan.innerText || userSpan.textContent || '').trim(); " +
                            "return ''; " +
                            "})()"
                    ) { result ->
                        val nameInDom = result?.trim('"', ' ', '\'').orEmpty().ifEmpty { "Пользователь" }
                        cookieManager.flush()
                        onSuccess(nameInDom, sessionId)
                        onDismiss()
                    }
                    break
                }
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
                    isManualMode = isManualMode,
                    onToggleMode = { isManualMode = !isManualMode },
                    onVerifyClick = { verifyAndCompleteManualCheck() },
                    onReloadClick = { webViewInstance?.reload() },
                    onDismiss = onDismiss,
                    topInset = topInset
                )

                Text(
                    text = if (isManualMode) {
                        "Введите API Key или sessionid для авторизации без прохождения капчи"
                    } else {
                        "Авторизация снимает ограничения частоты запросов к REST API"
                    },
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF222222))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                if (isManualMode) {
                    NLoginManualInput(
                        tokenText = manualToken,
                        usernameText = manualUsername,
                        onTokenChange = { manualToken = it },
                        onUsernameChange = { manualUsername = it },
                        onSubmit = {
                            val cleanUser = manualUsername.trim().ifEmpty { "Пользователь" }
                            NAuthState.onManualAuth(manualToken, cleanUser)
                            onSuccess(cleanUser, manualToken.trim())
                            onDismiss()
                        }
                    )
                } else {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                setupNLoginWebView(this) { user, session ->
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
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupNLoginWebView(
    webView: WebView,
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
        allowFileAccess = true
        allowContentAccess = true
        javaScriptCanOpenWindowsAutomatically = true
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_DEFAULT
        userAgentString = NUserAgent.current
    }

    webView.webChromeClient = WebChromeClient()
    webView.webViewClient = object : WebViewClient() {
        private var handled = false

        private fun checkAndDetectLogin(view: WebView?, currentUrl: String?) {
            if (handled) return

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
                    val cleanName = result?.trim('"', ' ', '\'').orEmpty().ifEmpty { "Пользователь" }
                    handled = true
                    cookieManager.flush()
                    onLoginDetected(cleanName, sessionId)
                }
            } else {
                view?.evaluateJavascript(
                    "(function() { " +
                        "var user = document.querySelector('a[href*=\"/users/\"]'); " +
                        "var logout = document.querySelector('a[href*=\"/logout\"]'); " +
                        "if (logout || user) { " +
                        "  return user ? (user.innerText || user.textContent || '').trim() : 'Пользователь'; " +
                        "} " +
                        "return ''; " +
                        "})()"
                ) { result ->
                    val nameInDom = result?.trim('"', ' ', '\'').orEmpty()
                    if (nameInDom.isNotEmpty() && !handled) {
                        cookieManager.flush()
                        val retryCookies = NCookieHelper.getAllCookies(cookieManager, view.url)
                        val retrySession = NCookieHelper.extractCookieValue(retryCookies, "sessionid")
                        val token = retrySession.ifEmpty {
                            NCookieHelper.extractCookieValue(retryCookies, "csrftoken").ifEmpty { "session" }
                        }
                        handled = true
                        onLoginDetected(nameInDom, token)
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
