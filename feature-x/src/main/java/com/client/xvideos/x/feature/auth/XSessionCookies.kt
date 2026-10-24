package com.client.xvideos.x.feature.auth

import android.webkit.CookieManager
import com.client.xvideos.x.urlStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import timber.log.Timber
import java.net.URI
import kotlin.coroutines.resume

/**
 * Cookie, которые сайт ставит при входе. Выход и новый вход снимают их все:
 * анонимную `session_token` сайт выдаст заново на первом же запросе.
 */
private val SESSION_COOKIE_NAMES = listOf(
    "session_token",
    "session_token_auth",
    "session_ath",
    "session_blih",
    "sess_pwd_pwnd",
)

/** Хост сайта X из [urlStart]: `www.<домен>`. */
private val xHost: String by lazy { URI(urlStart).host.orEmpty() }

/** Домен cookie сайта X: сайт ставит их на `.<домен>`, без `www`. */
private val xCookieDomain: String by lazy { xHost.removePrefix("www.") }

/** Хост относится к сайту X: сам сайт или его поддомен. */
internal fun isXHost(host: String): Boolean =
    host.isNotEmpty() && (host == xCookieDomain || host.endsWith(".$xCookieDomain"))

/** Хранилище cookie WebView; `null`, если WebView в системе недоступен (обновляется, отключён). */
internal fun webViewCookiesOrNull(): CookieManager? =
    runCatching { CookieManager.getInstance() }
        .onFailure { Timber.w("X: хранилище cookie WebView недоступно: ${it.javaClass.simpleName}") }
        .getOrNull()

/**
 * Cookie прямых запросов X берутся из хранилища WebView и туда же ложатся.
 *
 * Сессию X открывает вход в WebView (капчу решает только страница сайта), а
 * ленту и поиск X грузят WebView, остальное — прямые запросы. С одним
 * хранилищем на всех запросы X идут от одного и того же пользователя сайта,
 * как в браузере. Чужих хостов jar не касается.
 */
internal object XWebViewCookieJar : CookieJar {

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!isXHost(url.host)) return emptyList()
        val header = webViewCookiesOrNull()?.getCookie(url.toString()) ?: return emptyList()
        return header.split(';').mapNotNull { Cookie.parse(url, it.trim()) }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty() || !isXHost(url.host)) return
        val manager = webViewCookiesOrNull() ?: return
        cookies.forEach { manager.setCookie(url.toString(), it.toString()) }
    }
}

/**
 * Снимает cookie сессии X в хранилище WebView и дожидается записи.
 *
 * Остальные cookie сайта (выбранная страна и прочее) остаются на месте:
 * `removeAllCookies` сбросил бы и их.
 */
internal suspend fun clearXSessionCookies() = withContext(Dispatchers.Main) {
    val manager = webViewCookiesOrNull() ?: return@withContext
    SESSION_COOKIE_NAMES.forEach { name ->
        // Сайт ставит cookie на домен, но на всякий случай снимаем и хостовые.
        manager.setCookieAwait("$name=; Max-Age=0; Path=/; Domain=.$xCookieDomain")
        manager.setCookieAwait("$name=; Max-Age=0; Path=/")
    }
    manager.flush()
}

private suspend fun CookieManager.setCookieAwait(value: String) {
    suspendCancellableCoroutine { continuation ->
        setCookie(urlStart, value) { if (continuation.isActive) continuation.resume(Unit) }
    }
}
