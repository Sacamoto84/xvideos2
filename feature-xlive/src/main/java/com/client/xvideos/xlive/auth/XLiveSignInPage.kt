package com.client.xvideos.xlive.auth

import com.client.xvideos.xlive.xLiveUrlStart
import java.net.URI

/** Страница входа сайта: форма с капчей, её ведут скрипты сайта. */
const val X_LIVE_SIGN_IN_URL = "$xLiveUrlStart/auth/login/"

/**
 * Служебные страницы входа и аккаунта — по списку `AUTH_PATHS` из скриптов
 * сайта. После формы сайт может увести на одну из них: проверка второго
 * фактора (`/security/tfa/auth/`), возраст, вход через другой сайт. Cookie
 * сессии к этому моменту уже стоит, но вход ещё не закончен.
 */
private val SIGN_IN_STEP_PREFIXES = listOf(
    "/auth/",
    "/auth-sso/",
    "/security/",
    "/accounts/",
    "/mobile/accounts/",
    "/oauth/",
    "/agegate/",
    "/identity/",
)

/**
 * Шаг входа для журнала: служебный раздел страницы, «сайт» или «другой сайт»
 * (вход через чужой аккаунт идёт через его сайт) — без остального пути.
 */
internal fun signInStep(url: String): String {
    val uri = runCatching { URI(url) }.getOrNull() ?: return "другой сайт"
    if (!isXLiveHost(uri.host.orEmpty())) return "другой сайт"
    val path = uri.path.orEmpty()
    return SIGN_IN_STEP_PREFIXES.firstOrNull { path.startsWith(it) } ?: "сайт"
}

/**
 * WebView ушёл со страниц входа: [url] — сайт, но не служебная страница, и
 * cookie сессии есть ([hasSession]). Это ещё не вход: сайт мог и не узнать
 * сессию, — его подтверждает запрос подписок ([XLiveSession.confirmSignIn]).
 */
internal fun isPastSignInPages(url: String, hasSession: Boolean): Boolean {
    if (!hasSession) return false
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    val path = uri.path.orEmpty()
    return isXLiveHost(uri.host.orEmpty()) && SIGN_IN_STEP_PREFIXES.none { path.startsWith(it) }
}
