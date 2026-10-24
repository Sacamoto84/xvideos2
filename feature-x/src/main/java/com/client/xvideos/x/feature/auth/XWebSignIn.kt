package com.client.xvideos.x.feature.auth

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.common.net.SiteMaintenanceReporter
import com.client.xvideos.x.feature.net.decodeJsStringResult
import com.client.xvideos.x.feature.net.isMainFrameMaintenance
import com.client.xvideos.x.urlStart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume

/** Сколько ждём загрузку страницы входа. */
private const val PAGE_LOAD_TIMEOUT_MS = 45_000L

/** Сколько ждём, пока скрипт сайта создаст виджет капчи. */
private const val FORM_READY_TIMEOUT_MS = 30_000L

/**
 * Сколько ждём решение капчи. Сложность задачи выбирает сайт: в браузере без
 * окна на сервере решение шло 4–6 минут, на телефоне ожидаются секунды.
 */
private const val CAPTCHA_TIMEOUT_MS = 300_000L

/** Сколько ждём ответ сайта на отправленную форму. */
private const val SUBMIT_TIMEOUT_MS = 30_000L

private const val POLL_INTERVAL_MS = 500L

/**
 * Вход в X через страницу входа сайта в WebView, см. [XSignInScripts].
 *
 * Обычным POST-ом войти нельзя: сайт требует решённую капчу, а решает её
 * только скрипт страницы. Поэтому вход открывает `/account` в WebView,
 * заполняет форму, запускает капчу сайта, дожидается её решения и отправляет
 * форму. Cookie сессии ложатся в общее хранилище WebView — их видят и лента в
 * WebView, и прямые запросы X ([XWebViewCookieJar]).
 *
 * WebView создаётся на время входа и уничтожается в конце. Пока вход идёт, его
 * получает [onWebView]: форма входа держит его в своём окне невидимым. Без
 * окна страница считается скрытой, как вкладка в фоне, и её таймеры
 * притормаживаются; автоматический вход обходится без окна.
 *
 * @param onStep сообщает этап входа; вызывается на главном потоке.
 * @param onWebView получает WebView входа, пока он жив, и `null` перед его уничтожением.
 */
internal suspend fun signInThroughSitePage(
    login: String,
    password: String,
    onStep: (XSignInStep) -> Unit,
    onWebView: (WebView?) -> Unit,
): XSignInResult = withContext(Dispatchers.Main) {
    val client = SignInPageClient()
    val webView = createSignInWebView(client)
    onWebView(webView)
    try {
        runSignIn(webView, client, login, password, onStep)
    } finally {
        onWebView(null)
        destroySignInWebView(webView)
    }
}

private suspend fun runSignIn(
    webView: WebView,
    client: SignInPageClient,
    login: String,
    password: String,
    onStep: (XSignInStep) -> Unit,
): XSignInResult {
    onStep(XSignInStep.OPENING)
    webView.loadUrl(urlStart + XSignInScripts.SIGN_IN_PAGE_PATH)
    val loaded = withTimeoutOrNull(PAGE_LOAD_TIMEOUT_MS) { client.loaded.await() } ?: false
    if (!loaded) return XSignInResult.Failed("Страница входа не загрузилась")

    onStep(XSignInStep.CAPTCHA)
    val fill = pollUntil(FORM_READY_TIMEOUT_MS) {
        webView.evaluate(XSignInScripts.fillFormAndStartCaptcha(login, password))
            .takeIf { it != XSignInScripts.FILL_WAIT && it.isNotEmpty() }
    }
    if (fill != XSignInScripts.FILL_STARTED) {
        Timber.w("X: форма входа не готова: %s", fill ?: "timeout")
        return XSignInResult.Failed("Форма входа на сайте не найдена")
    }

    val captcha = pollUntil(CAPTCHA_TIMEOUT_MS) {
        captchaProgress(webView.evaluate(XSignInScripts.CAPTCHA_RESPONSE)).takeIf { it != CaptchaProgress.PENDING }
    }
    when (captcha) {
        CaptchaProgress.SOLVED -> Unit
        null -> return XSignInResult.Failed("Сайт слишком долго проверял устройство. Попробуйте ещё раз")
        else -> return XSignInResult.Failed("Сайт не пропустил проверку устройства. Попробуйте ещё раз")
    }

    onStep(XSignInStep.SENDING)
    webView.evaluate(XSignInScripts.SUBMIT)
    val payload = pollUntil(SUBMIT_TIMEOUT_MS) {
        webView.evaluate(XSignInScripts.SUBMIT_RESULT).takeIf { it.isNotEmpty() }
    } ?: return XSignInResult.Failed("Сайт не ответил на вход")

    webViewCookiesOrNull()?.flush()
    return parseSignInPayload(payload)
}

/** Состояние капчи по значению её скрытого поля. */
internal enum class CaptchaProgress { PENDING, SOLVED, FAILED }

/**
 * Решение капчи — непустое значение без точки впереди; с точкой — состояние
 * виджета. Ошибку, истёкшее решение, сброс и уничтоженный виджет сам он не
 * перезапускает (`data-start="none"`), ждать их бесполезно.
 */
internal fun captchaProgress(value: String): CaptchaProgress = when {
    value.isEmpty() -> CaptchaProgress.PENDING
    !value.startsWith('.') -> CaptchaProgress.SOLVED
    FAILED_CAPTCHA_STATES.any { value.startsWith(it) } -> CaptchaProgress.FAILED
    else -> CaptchaProgress.PENDING
}

private val FAILED_CAPTCHA_STATES = listOf(".ERROR", ".EXPIRED", ".RESET", ".DESTROYED")

/** Повторяет [probe] раз в [POLL_INTERVAL_MS], пока он не вернёт значение; `null` — время вышло. */
private suspend fun <T : Any> pollUntil(timeoutMs: Long, probe: suspend () -> T?): T? =
    withTimeoutOrNull(timeoutMs) {
        var value = probe()
        while (value == null) {
            delay(POLL_INTERVAL_MS)
            value = probe()
        }
        value
    }

private suspend fun WebView.evaluate(script: String): String = suspendCancellableCoroutine { continuation ->
    evaluateJavascript(script) { raw ->
        if (continuation.isActive) continuation.resume(decodeJsStringResult(raw))
    }
}

/** Загрузка главного фрейма страницы входа: `true` — загрузилась, `false` — ошибка. */
private class SignInPageClient : WebViewClient() {

    val loaded = CompletableDeferred<Boolean>()

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        loaded.complete(true)
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            Timber.w("X: страница входа не загрузилась: ${error?.errorCode}")
            loaded.complete(false)
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame != true) return
        val statusCode = errorResponse?.statusCode ?: return
        Timber.w("X: страница входа ответила HTTP $statusCode")
        if (isMainFrameMaintenance(true, statusCode)) SiteMaintenanceReporter.report("X")
        loaded.complete(false)
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createSignInWebView(client: SignInPageClient): WebView {
    val webView = WebView(AppContextHolder.applicationContext)
    // Поля формы сайта заполняет скрипт, службе автозаполнения их видеть незачем:
    // пока форма входа открыта, окно автозаполнению открыто.
    webView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    // Скрипт страницы может поставить фокус в своё поле — фокус и клавиатура
    // должны остаться у полей формы приложения.
    webView.isFocusable = false
    webView.isFocusableInTouchMode = false
    webViewCookiesOrNull()?.let { manager ->
        manager.setAcceptCookie(true)
        // Капче сайта сторонние cookie не нужны: она держит сессию в адресах запросов.
        manager.setAcceptThirdPartyCookies(webView, false)
    }
    // JavaScript включён намеренно: форму и капчу ведут скрипты страницы.
    // Моста в приложение нет — ни одного @JavascriptInterface, — итог входа
    // забирается через evaluateJavascript.
    with(webView.settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // Картинки страницы входу не нужны.
        loadsImagesAutomatically = false
        blockNetworkImage = true
    }
    // Консоль страницы в журнал не идёт: WebView печатает её вместе с адресом.
    webView.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean = true
    }
    webView.webViewClient = client
    return webView
}

private fun destroySignInWebView(webView: WebView) {
    // WebView уничтожают только вне иерархии View: форма могла держать его в окне.
    (webView.parent as? ViewGroup)?.removeView(webView)
    runCatching {
        webView.stopLoading()
        webView.webViewClient = WebViewClient()
        webView.destroy()
    }.onFailure { Timber.w("X: WebView входа не уничтожен: ${it.javaClass.simpleName}") }
}
