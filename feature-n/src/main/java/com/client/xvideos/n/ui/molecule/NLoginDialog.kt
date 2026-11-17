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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.net.NAuthState
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.net.NUserAgent
import com.client.xvideos.n.ui.atom.NLoginManualInput

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
                // Верхняя панель
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2B2B2B))
                ) {
                    if (topInset > 0.dp) {
                        Spacer(Modifier.height(topInset))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isManualMode) "Ввод ключа / Cookie" else "Вход в nhentai (Лимиты API)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (!isManualMode) {
                            IconButton(onClick = { webViewInstance?.reload() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Обновить страницу",
                                    tint = Color.White
                                )
                            }
                        }
                        TextButton(onClick = { isManualMode = !isManualMode }) {
                            Text(
                                text = if (isManualMode) "Веб-форма" else "Ключ / Cookie",
                                color = Color(0xFFED2553),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Информационная подсказка
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
                    // WebView для входа на nhentai.net
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                setupNLoginWebView(this, onSuccess)
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

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (handled) return

            val cookies = cookieManager.getCookie("https://nhentai.net")
            if (!cookies.isNullOrBlank()) {
                if (cookies.contains("cf_clearance=")) {
                    NCloudflareState.onChallengeCompleted(cookies)
                }
                if (cookies.contains("sessionid=")) {
                    val sessionId = cookies.split(";")
                        .map { it.trim() }
                        .firstOrNull { it.startsWith("sessionid=") }
                        ?.substringAfter("sessionid=")
                        .orEmpty()

                    if (sessionId.isNotBlank()) {
                        // Извлекаем имя пользователя из веб-страницы
                        view?.evaluateJavascript(
                            "(function() { " +
                                "var el = document.querySelector('.username') || document.querySelector('a[href^=\"/users/\"]'); " +
                                "return el ? el.innerText.trim() : ''; " +
                                "})()"
                        ) { result ->
                            val cleanName = result?.trim('"', ' ', '\'').orEmpty().ifEmpty { "Пользователь" }
                            handled = true
                            cookieManager.flush()
                            onLoginDetected(cleanName, sessionId)
                        }
                    }
                }
            }
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
