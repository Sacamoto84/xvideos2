package com.client.xvideos.n.ui.molecule

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.client.xvideos.n.net.NCookieHelper
import com.client.xvideos.n.net.NUserAgent
import com.client.xvideos.n.net.isNTrustedWebUrl
import com.client.xvideos.n.ui.atom.NCloudflareChallengeHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
            Column(modifier = Modifier.fillMaxSize()) {
                NCloudflareChallengeHeader(topInset = topInset, onDismiss = null)
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Проверка Cloudflare", color = Color.LightGray)
                }
            }
        }
        return
    }

    val handled = remember(url, rejectedClearance) { AtomicBoolean(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val ioScope = rememberCoroutineScope()
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentSuccess by rememberUpdatedState(onSuccess)
    val dismiss = {
        if (handled.compareAndSet(false, true)) currentDismiss()
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

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
            Column(modifier = Modifier.fillMaxSize()) {
                NCloudflareChallengeHeader(topInset = topInset, onDismiss = dismiss)

                // Интерактивный WebView для подтверждения "Verify you are human"
                key(url, rejectedClearance) {
                    AndroidView(
                        factory = { context ->
                            createChallengeWebView(
                                context = context,
                                url = url,
                                rejectedClearance = rejectedClearance,
                                flushCookiesAsync = flushCookiesAsync,
                                onCreated = { webViewInstance = it },
                                onUntrustedUrl = dismiss,
                                onClearanceFound = { cookies ->
                                    if (handled.compareAndSet(false, true)) currentSuccess(cookies)
                                },
                            )
                        },
                        onRelease = { webView ->
                            handled.set(true)
                            webViewInstance = null
                            releaseChallengeWebView(webView)
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

private fun createChallengeWebView(
    context: Context,
    url: String,
    rejectedClearance: String,
    flushCookiesAsync: () -> Unit,
    onCreated: (WebView) -> Unit,
    onUntrustedUrl: () -> Unit,
    onClearanceFound: (String) -> Unit,
): WebView = WebView(context).apply {
    onCreated(this)
    if (!isNTrustedWebUrl(url)) {
        onUntrustedUrl()
    } else {
        setupCloudflareWebView(
            webView = this,
            url = url,
            rejectedClearance = rejectedClearance,
            flushCookiesAsync = flushCookiesAsync,
            onClearanceFound = onClearanceFound,
        )
    }
}

private fun releaseChallengeWebView(webView: WebView) {
    runCatching {
        webView.stopLoading()
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.destroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupCloudflareWebView(
    webView: WebView,
    url: String,
    rejectedClearance: String,
    flushCookiesAsync: () -> Unit,
    onClearanceFound: (String) -> Unit
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

        private fun checkClearance(finishedUrl: String?) {
            if (finishedUrl == null || !isNTrustedWebUrl(finishedUrl)) return
            val cookies = NCookieHelper.getAllCookies(cookieManager, finishedUrl)
            val cf = NCookieHelper.extractCookieValue(cookies, "cf_clearance")
            if (cf.isNotEmpty() && cf != rejectedClearance) {
                flushCookiesAsync()
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
