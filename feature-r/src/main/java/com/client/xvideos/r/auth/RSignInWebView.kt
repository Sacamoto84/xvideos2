package com.client.xvideos.r.auth

import android.annotation.SuppressLint
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.webkit.ProfileStore
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.client.xvideos.common.util.runCatchingCancellable
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
private const val NETWORK_ERROR = "Не удалось открыть страницу входа RedGIFs."
private const val SESSION_ERROR = "Не удалось подтвердить вход в RedGIFs."
private const val PROFILE_ERROR = "RedGIFs подтвердил вход, но не удалось загрузить профиль."

@Composable
fun RSignInWebView(onSignedIn: () -> Unit, modifier: Modifier = Modifier) {
    val supported = remember {
        WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) &&
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
    }
    if (!supported) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Для входа обновите Android System WebView.", Modifier.padding(16.dp))
        }
        return
    }

    var attempt by remember { mutableIntStateOf(0) }
    key(attempt) {
        RSignInAttempt(onSignedIn, onRetry = { attempt++ }, modifier)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RSignInAttempt(onSignedIn: () -> Unit, onRetry: () -> Unit, modifier: Modifier) {
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
                TextButton(onClick = onRetry) { Text("Повторить") }
            }
        } else {
            AndroidView(
                factory = { context ->
                    val store = ProfileStore.getInstance()
                    store.allProfileNames.filter { it.startsWith(PROFILE_PREFIX) }.forEach { removeProfile(it) }
                    WebView(context).apply {
                        WebViewCompat.setProfile(this, profile)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setSupportMultipleWindows(true)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                        val signInClient = RedgifsSignInClient(
                            onLoading = { loading = it },
                            onError = { error = it },
                        )
                        webViewClient = signInClient
                        WebViewCompat.addWebMessageListener(this, BRIDGE, setOf(WEB_ORIGIN)) {
                                view, reply, origin, isMainFrame, _ ->
                            if (!isMainFrame || !origin.isHttpsHost(WEB_HOST) ||
                                !signInClient.isReturningFromProvider || saving || error != null ||
                                !Uri.parse(view.url.orEmpty()).isHttpsHost(WEB_HOST)
                            ) return@addWebMessageListener
                            if (reply.type != WebMessageCompat.TYPE_STRING) return@addWebMessageListener
                            val payload = reply.data.orEmpty()
                            if (payload.length > 17_000) return@addWebMessageListener
                            if (payload == "profile-unavailable") {
                                error = PROFILE_ERROR
                                return@addWebMessageListener
                            }
                            val credentials = runCatching { Json.decodeFromString<RAccountCredentials>(payload) }.getOrNull()
                            if (credentials == null) {
                                error = SESSION_ERROR
                                return@addWebMessageListener
                            }
                            saving = true
                            scope.launch {
                                if (runCatchingCancellable { RAccountSession.save(credentials) }.getOrDefault(false)) {
                                    currentOnSignedIn()
                                } else {
                                    error = "Не удалось сохранить сессию RedGIFs."
                                }
                            }
                        }
                        doOnLayout { loadUrl(LOGIN_URL) }
                    }
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

private fun removeProfile(name: String) {
    runCatching { ProfileStore.getInstance().deleteProfile(name) }
        .onFailure { Timber.w("Не удалось очистить профиль WebView для входа в RedGIFs") }
}

private class RedgifsSignInClient(
    private val onLoading: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
) : WebViewClient() {
    private var openedSignInRoute = false
    var isReturningFromProvider = false
        private set

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        request.isForMainFrame && !request.url.isSignInHost()

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        val uri = Uri.parse(url)
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
            Uri.parse(url).isHttpsHost(WEB_HOST) && Uri.parse(currentUrl.orEmpty()).isHttpsHost(WEB_HOST)
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
