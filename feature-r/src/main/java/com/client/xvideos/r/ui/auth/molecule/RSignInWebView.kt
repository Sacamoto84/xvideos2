package com.client.xvideos.r.ui.auth.molecule

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import androidx.webkit.ProfileStore
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.auth.RAccountCredentials
import com.client.xvideos.r.auth.RAccountSession
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID

private const val WEB_HOST = "www.redgifs.com"
private const val WEB_ORIGIN = "https://$WEB_HOST"
private const val LOGIN_URL = "$WEB_ORIGIN/login"
private const val PROVIDER_HOST = "auth2.redgifs.com"
private const val BRIDGE = "androidRedgifsSignIn"
private const val PROFILE_PREFIX = "redgifs-signin-"
private const val NETWORK_ERROR = "Не удалось открыть страницу входа R."
private const val SESSION_ERROR = "Не удалось подтвердить вход в R."
private const val PROFILE_ERROR = "R подтвердил вход, но не удалось загрузить профиль."

@Composable
fun RSignInWebView(onSignedIn: () -> Unit, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Окно входа в R", Modifier.padding(16.dp))
        }
        return
    }
    val supported = remember { isSignInWebViewSupported() }
    if (!supported) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Для входа обновите Android System WebView.", Modifier.padding(16.dp))
        }
        return
    }

    var attempt by remember { mutableIntStateOf(0) }
    key(attempt) {
        val scope = rememberCoroutineScope()
        val currentOnSignedIn by rememberUpdatedState(onSignedIn)
        val profile = remember { PROFILE_PREFIX + UUID.randomUUID() }
        var loading by remember { mutableStateOf(true) }
        var saving by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        Box(modifier, contentAlignment = Alignment.Center) {
            val message = error
            if (message != null) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(message)
                    TextButton(onClick = { attempt++ }) { Text("Повторить") }
                }
            } else {
                AndroidView(
                    factory = { context ->
                        buildSignInWebView(
                            context = context,
                            profile = profile,
                            onLoading = { loading = it },
                            onError = { error = it },
                            canAcceptMessage = { !saving && error == null },
                            onCredentials = { credentials ->
                                saving = true
                                scope.launch {
                                    if (runCatchingCancellable { RAccountSession.save(credentials) }.getOrDefault(false)) {
                                        currentOnSignedIn()
                                    } else {
                                        error = "Не удалось сохранить сессию R."
                                    }
                                }
                            },
                        )
                    },
                    modifier = Modifier.matchParentSize(),
                    onRelease = { view ->
                        view.stopLoading()
                        view.destroy()
                        removeProfile(profile)
                    },
                )
                if (loading || saving) CircularProgressIndicator()
            }
        }
    }
}

private fun isSignInWebViewSupported(): Boolean =
    WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) &&
        WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)

@SuppressLint("SetJavaScriptEnabled")
private fun buildSignInWebView(
    context: Context,
    profile: String,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    canAcceptMessage: () -> Boolean,
    onCredentials: (RAccountCredentials) -> Unit,
): WebView {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
        ProfileStore.getInstance().allProfileNames
            .filter { it.startsWith(PROFILE_PREFIX) }
            .forEach(::removeProfile)
    }
    return WebView(context).apply {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            WebViewCompat.setProfile(this, profile)
        }
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setSupportMultipleWindows(true)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        val signInClient = RSignInClient(onLoading = onLoading, onError = onError)
        webViewClient = signInClient
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(this, BRIDGE, setOf(WEB_ORIGIN)) {
                    view, reply, origin, isMainFrame, _ ->
                if (!shouldAcceptSignInMessage(
                        isMainFrame = isMainFrame,
                        origin = origin,
                        returningFromProvider = signInClient.isReturningFromProvider,
                        canAccept = canAcceptMessage(),
                        pageUrl = view.url.orEmpty(),
                        replyType = reply.type,
                    )
                ) return@addWebMessageListener
                handleSignInPayload(reply.data.orEmpty(), onError, onCredentials)
            }
        }
        doOnLayout { loadUrl(LOGIN_URL) }
    }
}

private fun shouldAcceptSignInMessage(
    isMainFrame: Boolean,
    origin: Uri,
    returningFromProvider: Boolean,
    canAccept: Boolean,
    pageUrl: String,
    replyType: Int,
): Boolean {
    if (!isMainFrame || !returningFromProvider || !canAccept) return false
    if (replyType != WebMessageCompat.TYPE_STRING) return false
    return origin.isHttpsHost(WEB_HOST) && pageUrl.toUri().isHttpsHost(WEB_HOST)
}

private fun handleSignInPayload(
    payload: String,
    onError: (String) -> Unit,
    onCredentials: (RAccountCredentials) -> Unit,
) {
    if (payload.length > 17_000) return
    if (payload == "profile-unavailable") {
        onError(PROFILE_ERROR)
        return
    }
    val credentials = runCatching { Json.decodeFromString<RAccountCredentials>(payload) }.getOrNull()
    if (credentials == null) {
        onError(SESSION_ERROR)
        return
    }
    onCredentials(credentials)
}

private fun removeProfile(name: String) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) return
    runCatching { ProfileStore.getInstance().deleteProfile(name) }
        .onFailure { Timber.w("Не удалось очистить профиль WebView для входа в R") }
}

private class RSignInClient(
    private val onLoading: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
) : WebViewClient() {
    private var openedSignInRoute = false
    var isReturningFromProvider = false
        private set

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        request.isForMainFrame && !request.url.isSignInHost()

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        val uri = url.toUri()
        if (!uri.isSignInHost()) {
            view.stopLoading()
            onError(NETWORK_ERROR)
            return
        }
        if (uri.isHttpsHost(PROVIDER_HOST)) isReturningFromProvider = true
        onLoading(true)
    }

    override fun onPageFinished(view: WebView, url: String) {
        val currentUrl = view.url
        val finishedOnSite =
            url.toUri().isHttpsHost(WEB_HOST) && currentUrl.orEmpty().toUri().isHttpsHost(WEB_HOST)
        if (finishedOnSite) {
            if (isReturningFromProvider) {
                onLoading(true)
                view.evaluateJavascript(READ_ACCOUNT_SESSION, null)
                return
            }
            if (!openedSignInRoute) {
                openedSignInRoute = true
                view.evaluateJavascript(OPEN_SIGN_IN_ROUTE, null)
            }
        }
        if (finishedOnSite || currentUrl == url) onLoading(false)
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) onError(NETWORK_ERROR)
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
        if (request.isForMainFrame) onError(NETWORK_ERROR)
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onError(NETWORK_ERROR)
        return true
    }
}

private fun Uri.isSignInHost(): Boolean = isHttpsHost(WEB_HOST) || isHttpsHost(PROVIDER_HOST)

private fun Uri.isHttpsHost(expectedHost: String): Boolean =
    scheme == "https" && host == expectedHost && (port == -1 || port == 443) && userInfo == null

// /login serves the SPA shell. Start its sign-in route and use the site's own
// measured viewport height when WebView's CSS viewport units resolve to zero.
private val OPEN_SIGN_IN_ROUTE = """
    (() => {
        if (location.origin !== '$WEB_ORIGIN') return;
        const style = document.createElement('style');
        style.textContent = `
            .routeWrapper > [class^="_wrapper_"]:has(> [class^="_formWrapper_"]) {
                height: var(--app-height, 100vh);
                align-items: safe center;
            }
        `;
        document.head.append(style);
        if (location.pathname !== '/auth/signin') {
            history.replaceState(history.state, '', '/auth/signin');
            dispatchEvent(new PopStateEvent('popstate', {state: history.state}));
        }
    })();
""".trimIndent()

// The site completes PKCE. Its client and API method names survive changes to the
// minified export names. Wait for the redirect exchange and verify /v1/me before returning a session.
private val READ_ACCOUNT_SESSION = """
    (async () => {
        if (location.origin !== '$WEB_ORIGIN' || window.redgifsAndroidCheckingSession) return;
        window.redgifsAndroidCheckingSession = true;
        let tokenReceived = false;
        const readSession = async () => {
            const bundle = [...document.querySelectorAll('link[rel="modulepreload"]')]
                .map(link => new URL(link.href))
                .find(url => url.origin === location.origin && /^\/static\/common-[\w-]+\.js${'$'}/.test(url.pathname));
            if (!bundle) throw new Error();
            const clients = Object.values(await import(bundle.href));
            const account = clients.find(client => typeof client?.isAuthenticated === 'function' &&
                typeof client.getIdToken === 'function');
            const profile = clients.find(client => typeof client?.getMe === 'function');
            if (!account || !profile) throw new Error();
            for (let attempt = 0; attempt < 24; attempt++) {
                try {
                    if (await account.isAuthenticated()) {
                        const token = await account.getIdToken();
                        if (typeof token === 'string' && token) {
                            tokenReceived = true;
                            const {username} = await profile.getMe();
                            if (typeof username === 'string' && username.trim()) {
                                return JSON.stringify({token, username});
                            }
                        }
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

@Preview
@Composable
private fun RSignInWebViewPreview() {
    RSignInWebView(onSignedIn = {}, modifier = Modifier.fillMaxSize())
}
