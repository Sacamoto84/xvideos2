package com.client.xvideos.xlive.auth

import android.webkit.CookieManager
import android.webkit.WebSettings
import com.client.xvideos.common.AppContextHolder
import com.client.xvideos.xlive.xLiveUrlStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import timber.log.Timber
import java.net.URI
import kotlin.coroutines.resume

/** Cookie сессии: сайт ставит её при входе и снимает при выходе. */
internal const val SESSION_COOKIE = "sessionid"

/** Cookie CSRF: её значение POST-запросы повторяют в заголовке `X-CSRFToken`. */
internal const val CSRF_COOKIE = "csrftoken"

/** Хост сайта XLive из [xLiveUrlStart]: `www.<домен>`. */
private val xLiveHost: String by lazy { URI(xLiveUrlStart).host.orEmpty() }

/** Домен cookie сайта: сайт ставит их на `.<домен>`, без `www`. */
private val xLiveCookieDomain: String by lazy { xLiveHost.removePrefix("www.") }

/** Хост относится к сайту XLive: сам сайт или его поддомен. */
internal fun isXLiveHost(host: String): Boolean =
    host.isNotEmpty() && (host == xLiveCookieDomain || host.endsWith(".$xLiveCookieDomain"))

/** Хранилище cookie WebView; `null`, если WebView в системе недоступен (обновляется, отключён). */
internal fun xLiveWebViewCookies(): CookieManager? =
    runCatching { CookieManager.getInstance() }
        .onFailure { Timber.w("XLive: хранилище cookie WebView недоступно: ${it.javaClass.simpleName}") }
        .getOrNull()

/**
 * User-Agent WebView: запросы с сессией идут от имени браузера, который
 * входил; `null` — WebView недоступен.
 */
internal fun webViewUserAgent(): String? =
    runCatching { WebSettings.getDefaultUserAgent(AppContextHolder.applicationContext) }
        .onFailure { Timber.w("XLive: User-Agent WebView недоступен: ${it.javaClass.simpleName}") }
        .getOrNull()

/**
 * Значение cookie [name] из строки заголовка `Cookie` [header]: `a=1; b=2`.
 *
 * @return `null`, если такой cookie нет или она пустая.
 */
internal fun cookieValue(header: String, name: String): String? =
    header.split(';')
        .map { it.trim() }
        .firstOrNull { it.startsWith("$name=") }
        ?.substringAfter('=')
        ?.takeIf { it.isNotEmpty() }

/** Cookie сайта [name] из хранилища WebView; `null` — её нет. */
internal fun xLiveCookie(name: String): String? =
    xLiveWebViewCookies()?.getCookie(xLiveUrlStart)?.let { cookieValue(it, name) }

/**
 * Cookie запросов XLive берутся из хранилища WebView и туда же ложатся.
 *
 * Сессию открывает вход в WebView (капчу решает только страница сайта), а
 * данные раздел берёт прямыми запросами. С одним хранилищем на всех запросы
 * идут от одного пользователя сайта, как в браузере. Чужих хостов jar не
 * касается.
 */
internal object XLiveWebViewCookieJar : CookieJar {

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!isXLiveHost(url.host)) return emptyList()
        val header = xLiveWebViewCookies()?.getCookie(url.toString()) ?: return emptyList()
        return header.split(';').mapNotNull { Cookie.parse(url, it.trim()) }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty() || !isXLiveHost(url.host)) return
        val manager = xLiveWebViewCookies() ?: return
        cookies.forEach { manager.setCookie(url.toString(), it.toString()) }
    }
}

/**
 * Снимает cookie сессии в хранилище WebView и дожидается записи. Остальные
 * cookie сайта остаются: `csrftoken` сайт всё равно выдаст заново.
 */
internal suspend fun clearXLiveSessionCookie() = withContext(Dispatchers.Main) {
    val manager = xLiveWebViewCookies() ?: return@withContext
    // Сайт ставит cookie сессии на `.www.<домен>` и с Secure: снять её можно только
    // с тем же доменом и тоже с Secure. На всякий случай — и с домена без `www`.
    manager.setCookieAwait("$SESSION_COOKIE=; Max-Age=0; Path=/; Domain=.$xLiveHost; Secure")
    manager.setCookieAwait("$SESSION_COOKIE=; Max-Age=0; Path=/; Domain=.$xLiveCookieDomain; Secure")
    manager.flush()
    if (xLiveCookie(SESSION_COOKIE) != null) Timber.w("XLive: cookie сессии не снялась")
}

private suspend fun CookieManager.setCookieAwait(value: String) {
    suspendCancellableCoroutine { continuation ->
        setCookie(xLiveUrlStart, value) { if (continuation.isActive) continuation.resume(Unit) }
    }
}
