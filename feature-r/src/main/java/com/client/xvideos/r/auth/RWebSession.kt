package com.client.xvideos.r.auth

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.webkit.ProfileStore
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.common.util.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.UUID
import kotlin.coroutines.resume

internal const val R_WEB_HOST = "www.redgifs.com"
internal const val R_WEB_ORIGIN = "https://$R_WEB_HOST"
internal const val R_PROVIDER_HOST = "auth2.redgifs.com"
internal const val R_PROVIDER_ORIGIN = "https://$R_PROVIDER_HOST"
private const val BRIDGE = "androidRedgifsSignIn"
private const val PROFILE_PREFIX = "redgifs-signin-"
private val webProfilePattern = Regex("${PROFILE_PREFIX}[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")

internal fun newRWebProfileName() = PROFILE_PREFIX + UUID.randomUUID()
internal fun isRWebProfileName(name: String) = webProfilePattern.matches(name)

internal sealed interface RWebSessionResult {
    data class Renewed(val token: String) : RWebSessionResult
    data object Unavailable : RWebSessionResult
    data object Ended : RWebSessionResult
}

internal fun isRWebViewSupported(): Boolean =
    WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) &&
        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)

internal fun Uri.isRHttpsHost(expectedHost: String): Boolean =
    scheme == "https" && host == expectedHost && (port == -1 || port == 443) && userInfo == null

@SuppressLint("SetJavaScriptEnabled")
internal fun createRWebView(context: Context, profile: String): WebView = WebView(context).apply {
    WebViewCompat.setProfile(this, profile)
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
}

internal fun WebView.onRWebMessage(canAccept: () -> Boolean, onPayload: (String) -> Unit) {
    WebViewCompat.addWebMessageListener(this, BRIDGE, setOf(R_WEB_ORIGIN)) {
            view, message, origin, isMainFrame, _ ->
        if (!isMainFrame || message.type != WebMessageCompat.TYPE_STRING || !canAccept()) {
            return@addWebMessageListener
        }
        if (!origin.isRHttpsHost(R_WEB_HOST) || !view.url.orEmpty().toUri().isRHttpsHost(R_WEB_HOST)) {
            return@addWebMessageListener
        }
        message.data?.takeIf { it.length <= 17_000 }?.let(onPayload)
    }
}

internal suspend fun renewRWebSession(profile: String): RWebSessionResult = withContext(Dispatchers.Main) {
    val context = AppContextHolder.getOrNull() ?: return@withContext RWebSessionResult.Unavailable
    runCatchingCancellable {
        if (!isRWebViewSupported()) return@withContext RWebSessionResult.Unavailable
        if (profile !in ProfileStore.getInstance().allProfileNames) return@withContext RWebSessionResult.Ended
        val view = createRWebView(context, profile)
        try {
            val result = withTimeoutOrNull(22_000L) { readRenewedSession(view) }
                ?: RWebSessionResult.Unavailable
            if (result is RWebSessionResult.Renewed) flushRWebCookies(view)
            result
        } finally {
            view.destroy()
        }
    }.getOrDefault(RWebSessionResult.Unavailable)
}

internal suspend fun flushRWebCookies(view: WebView) {
    val cookies = WebViewCompat.getProfile(view).cookieManager
    withContext(Dispatchers.IO) { cookies.flush() }
}

internal suspend fun clearRWebProfile(name: String) = withContext(Dispatchers.Main) {
    runCatchingCancellable {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) return@withContext
        val store = ProfileStore.getInstance()
        if (runCatching { store.deleteProfile(name) }.isSuccess) return@withContext
        val profile = store.getProfile(name) ?: return@withContext
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)) {
            withTimeoutOrNull(5_000L) {
                suspendCancellableCoroutine { continuation ->
                    WebStorageCompat.deleteBrowsingData(profile.webStorage) { continuation.resume(Unit) }
                }
            }
        } else {
            profile.webStorage.deleteAllData()
            val cookies = profile.cookieManager
            withTimeoutOrNull(5_000L) {
                suspendCancellableCoroutine { continuation -> cookies.removeAllCookies { continuation.resume(Unit) } }
            }
            withContext(Dispatchers.IO) { cookies.flush() }
        }
    }.onFailure { Timber.w("Не удалось очистить профиль WebView для входа в R") }
    Unit
}

private suspend fun readRenewedSession(view: WebView): RWebSessionResult = suspendCancellableCoroutine { result ->
    val complete: (RWebSessionResult) -> Unit = { if (result.isActive) result.resume(it) }
    view.webViewClient = RSessionRenewalClient { complete(RWebSessionResult.Unavailable) }
    view.onRWebMessage(canAccept = { result.isActive }) { payload ->
        val renewal = when {
            payload == "session-ended" -> RWebSessionResult.Ended
            payload.isBlank() -> RWebSessionResult.Unavailable
            else -> RWebSessionResult.Renewed(payload)
        }
        complete(renewal)
    }
    view.loadUrl("$R_WEB_ORIGIN/")
}

private class RSessionRenewalClient(private val onError: () -> Unit) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        (request.isForMainFrame && !request.url.isRHttpsHost(R_WEB_HOST)).also { blocked ->
            if (blocked) onError()
        }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (!url.toUri().isRHttpsHost(R_WEB_HOST)) {
            onError()
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (url.toUri().isRHttpsHost(R_WEB_HOST) && view.url.orEmpty().toUri().isRHttpsHost(R_WEB_HOST)) {
            view.evaluateJavascript(readRWebSessionScript(renewing = true), null)
        }
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) onError()
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
        if (request.isForMainFrame) onError()
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onError()
        return true
    }
}

// The site's SDK completes PKCE on sign-in and restores its session from cookies on a fresh page.
internal fun readRWebSessionScript(renewing: Boolean) = """
    (async () => {
        if (location.origin !== '$R_WEB_ORIGIN' || window.redgifsAndroidCheckingSession) return;
        window.redgifsAndroidCheckingSession = true;
        const renewing = $renewing;
        const hasSessionCookie = () => document.cookie.split(';').some(cookie => cookie.trim().startsWith('_kbrte='));
        let tokenReceived = false;
        const readSession = async () => {
            const bundle = [...document.querySelectorAll('link[rel="modulepreload"]')]
                .map(link => new URL(link.href))
                .find(url => url.origin === location.origin && /^\/static\/common-[\w-]+\.js${'$'}/.test(url.pathname));
            if (!bundle) throw new Error();
            const clients = Object.values(await import(bundle.href));
            const account = clients.find(client => typeof client?.isAuthenticated === 'function' &&
                typeof client.getIdToken === 'function');
            if (!account) throw new Error();
            const profile = clients.find(client => typeof client?.getMe === 'function');
            if (!renewing && !profile) throw new Error();
            for (let attempt = 0; attempt < 24; attempt++) {
                try {
                    if (await account.isAuthenticated()) {
                        const token = await account.getIdToken();
                        if (typeof token === 'string' && token) {
                            if (renewing) return token;
                            tokenReceived = true;
                            const {username} = await profile.getMe();
                            if (typeof username === 'string' && username.trim()) {
                                return JSON.stringify({token, username});
                            }
                        }
                    } else if (renewing && !hasSessionCookie()) {
                        return 'session-ended';
                    }
                } catch (_) {}
                await new Promise(resolve => setTimeout(resolve, 500));
            }
        };
        let timer;
        let session;
        try {
            session = await Promise.race([
                readSession(),
                new Promise(resolve => { timer = setTimeout(resolve, 18000); }),
            ]);
        } catch (_) {
        } finally {
            clearTimeout(timer);
        }
        window.$BRIDGE.postMessage(session ?? (tokenReceived ? 'profile-unavailable' : ''));
    })();
""".trimIndent()
