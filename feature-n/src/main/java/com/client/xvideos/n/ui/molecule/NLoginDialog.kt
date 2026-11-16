package com.client.xvideos.n.ui.molecule

import android.annotation.SuppressLint
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.client.xvideos.common.net.UserAgentProvider
import com.client.xvideos.common.util.getTopInsetDp

/**
 * Диалог авторизации в nhentai через встроенный WebView с поддержкой Cloudflare Turnstile.
 */
@Composable
fun NLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: (username: String, sessionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = getTopInsetDp()

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
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Вход в nhentai (Лимиты API)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
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
                    text = "Авторизация снимает ограничения частоты запросов к REST API",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF222222))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                // WebView для входа на nhentai.net
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setupNLoginWebView(this, onSuccess)
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
    onLoginDetected: (username: String, sessionId: String) -> Unit
) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, true)

    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_DEFAULT
        userAgentString = UserAgentProvider.defaultUserAgent
    }

    webView.webChromeClient = WebChromeClient()
    webView.webViewClient = object : WebViewClient() {
        private var handled = false

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (handled) return

            val cookies = cookieManager.getCookie("https://nhentai.net")
            if (!cookies.isNullOrBlank() && cookies.contains("sessionid=")) {
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
                        onLoginDetected(cleanName, sessionId)
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
