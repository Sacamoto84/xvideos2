package com.client.xvideos.r.ui.auth.molecule

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.auth.RAccountCredentials
import com.client.xvideos.r.auth.RAccountSession
import com.client.xvideos.r.auth.R_PROVIDER_HOST
import com.client.xvideos.r.auth.R_WEB_HOST
import com.client.xvideos.r.auth.R_WEB_ORIGIN
import com.client.xvideos.r.auth.clearRWebProfile
import com.client.xvideos.r.auth.createRWebView
import com.client.xvideos.r.auth.flushRWebCookies
import com.client.xvideos.r.auth.isRHttpsHost
import com.client.xvideos.r.auth.isRWebViewSupported
import com.client.xvideos.r.auth.newRWebProfileName
import com.client.xvideos.r.auth.onRWebMessage
import com.client.xvideos.r.auth.readRWebSessionScript
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val LOGIN_URL = "$R_WEB_ORIGIN/login"
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
    val supported = remember { isRWebViewSupported() }
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
        val profile = remember { newRWebProfileName() }
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
                            onCredentials = { view, credentials ->
                                saving = true
                                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    val saved = withContext(NonCancellable) {
                                        runCatchingCancellable {
                                            flushRWebCookies(view)
                                            RAccountSession.save(credentials.copy(webProfile = profile))
                                        }.getOrDefault(false).also { success ->
                                            if (!success) clearRWebProfile(profile)
                                        }
                                    }
                                    if (saved) {
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
                        view.destroy()
                        if (!saving) runCatching { ProfileStore.getInstance().deleteProfile(profile) }
                    },
                )
                if (loading || saving) CircularProgressIndicator()
            }
        }
    }
}

private fun buildSignInWebView(
    context: Context,
    profile: String,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    canAcceptMessage: () -> Boolean,
    onCredentials: (WebView, RAccountCredentials) -> Unit,
): WebView = createRWebView(context, profile).apply {
    settings.setSupportMultipleWindows(true)
    val signInClient = RSignInClient(onLoading = onLoading, onError = onError)
    webViewClient = signInClient
    onRWebMessage(canAccept = { signInClient.isReturningFromProvider && canAcceptMessage() }) { payload ->
        handleSignInPayload(payload, onError) { onCredentials(this, it) }
    }
    doOnLayout { loadUrl(LOGIN_URL) }
}

private fun handleSignInPayload(
    payload: String,
    onError: (String) -> Unit,
    onCredentials: (RAccountCredentials) -> Unit,
) {
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
        if (uri.isRHttpsHost(R_PROVIDER_HOST)) isReturningFromProvider = true
        onLoading(true)
    }

    override fun onPageFinished(view: WebView, url: String) {
        val currentUrl = view.url
        val finishedOnSite =
            url.toUri().isRHttpsHost(R_WEB_HOST) && currentUrl.orEmpty().toUri().isRHttpsHost(R_WEB_HOST)
        if (finishedOnSite) {
            if (isReturningFromProvider) {
                onLoading(true)
                view.evaluateJavascript(readRWebSessionScript(renewing = false), null)
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

private fun Uri.isSignInHost(): Boolean = isRHttpsHost(R_WEB_HOST) || isRHttpsHost(R_PROVIDER_HOST)

// /login serves the SPA shell. Start its sign-in route and use the site's own
// measured viewport height when WebView's CSS viewport units resolve to zero.
private val OPEN_SIGN_IN_ROUTE = """
    (() => {
        if (location.origin !== '$R_WEB_ORIGIN') return;
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

@Preview
@Composable
private fun RSignInWebViewPreview() {
    RSignInWebView(onSignedIn = {}, modifier = Modifier.fillMaxSize())
}
