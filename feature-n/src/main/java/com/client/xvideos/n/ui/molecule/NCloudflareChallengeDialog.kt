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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.client.xvideos.n.net.NCookieHelper
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.net.NUserAgent
import com.client.xvideos.n.net.isNTrustedWebUrl
import com.client.xvideos.n.repository.CloudflareBlockedException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Интерактивный диалог проверки Cloudflare Turnstile через встроенный WebView.
 */
@Composable
fun NCloudflareChallengeDialog(
    url: String,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    modifier: Modifier = Modifier,
    rejectedClearance: String = ""
) {
    val topInset = getTopInsetDp()
    val handled = remember(url, rejectedClearance) { AtomicBoolean(false) }
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentSuccess by rememberUpdatedState(onSuccess)
    val dismiss = {
        if (handled.compareAndSet(false, true)) currentDismiss()
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
                // Верхняя панель управления с учётом выреза / статус-бара
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
                            text = "Подтверждение Cloudflare",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = dismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Интерактивный WebView для подтверждения "Verify you are human"
                key(url, rejectedClearance) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                setupCloudflareWebView(this, url, rejectedClearance) { cookies ->
                                    if (handled.compareAndSet(false, true)) currentSuccess(cookies)
                                }
                            }
                        },
                        onRelease = { webView ->
                            handled.set(true)
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

@Composable
internal fun NRequestChallengeDialog(
    challenge: CloudflareBlockedException,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    NCloudflareChallengeDialog(
        url = challenge.url,
        rejectedClearance = challenge.sentClearance,
        onDismiss = onDismiss,
        onSuccess = { cookies ->
            NCloudflareState.onChallengeCompleted(cookies)
            onVerified()
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupCloudflareWebView(
    webView: WebView,
    url: String,
    rejectedClearance: String,
    onClearanceFound: (String) -> Unit
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
        private fun checkClearance(finishedUrl: String?) {
            if (finishedUrl == null || !isNTrustedWebUrl(finishedUrl)) return
            val cookies = NCookieHelper.getAllCookies(cookieManager, finishedUrl)
            val cf = NCookieHelper.extractCookieValue(cookies, "cf_clearance")
            if (cf.isNotEmpty() && cf != rejectedClearance) {
                cookieManager.flush()
                onClearanceFound(cookies)
            }
        }

        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
            super.onPageFinished(view, finishedUrl)
            checkClearance(finishedUrl)
        }

    }

    webView.loadUrl(url)
}

@Preview
@Composable
private fun NCloudflareChallengeDialogPreview() {
    NCloudflareChallengeDialog(
        url = "https://nhentai.net",
        onDismiss = {},
        onSuccess = {}
    )
}
