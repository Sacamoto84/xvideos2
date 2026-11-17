package com.client.xvideos.n.net

import android.webkit.CookieManager

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
        val targets = if (currentUrl.isNullOrBlank()) {
            NHENTAI_URLS
        } else {
            listOf(currentUrl) + NHENTAI_URLS
        }

        return targets
            .mapNotNull { runCatching { cookieManager.getCookie(it) }.getOrNull() }
            .flatMap { it.split(";") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("; ")
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
