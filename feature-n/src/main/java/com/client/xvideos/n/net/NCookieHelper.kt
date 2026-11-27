package com.client.xvideos.n.net

import android.webkit.CookieManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Утилиты для надёжного извлечения cookie сайта nhentai.net из [CookieManager].
 */
object NCookieHelper {

    private val NHENTAI_URLS = listOf(
        "https://nhentai.net/",
        "https://nhentai.net/login/",
        "https://nhentai.net/favorites/",
        "https://nhentai.net"
    )

    /**
     * Возвращает объединённую строку всех cookie для nhentai с учётом путей и поддоменов.
     */
    fun getAllCookies(cookieManager: CookieManager, currentUrl: String? = null): String {
        val targets = listOfNotNull(currentUrl?.takeIf(::isNTrustedWebUrl)) + NHENTAI_URLS

        val byName = linkedMapOf<String, String>()
        targets
            .mapNotNull { runCatching { cookieManager.getCookie(it) }.getOrNull() }
            .flatMap { it.split(";") }
            .map { it.trim() }
            .filter { it.isNotEmpty() && '=' in it }
            .forEach { pair ->
                val nameKey = pair.substringBefore('=').trim().lowercase()
                if (nameKey.isNotEmpty() && !byName.containsKey(nameKey)) {
                    byName[nameKey] = pair
                }
            }
        return byName.values.joinToString("; ")
    }

    /**
     * Извлекает значение указанной куки по имени.
     */
    fun extractCookieValue(cookies: String, cookieName: String): String {
        return cookies.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$cookieName=", ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim()
            .orEmpty()
    }
}

internal fun isNTrustedWebUrl(url: String): Boolean =
    url.toHttpUrlOrNull()?.let { it.isHttps && it.host.equals("nhentai.net", ignoreCase = true) && it.port == 443 } == true
