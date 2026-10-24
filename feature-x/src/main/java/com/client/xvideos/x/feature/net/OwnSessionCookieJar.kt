package com.client.xvideos.x.feature.net

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Cookie одной отдельной сессии сайта: живут в памяти экземпляра и больше нигде.
 *
 * Общий клиент X пишет cookie в хранилище WebView ([com.client.xvideos.x.feature.auth.XWebViewCookieJar]):
 * там сессия входа и выбранная на сайте ориентация. Запрос, которому нужна
 * своя сессия (например, смена ориентации ради меню), берёт этот jar и
 * пользователю ничего не переключает.
 */
internal class OwnSessionCookieJar : CookieJar {

    private val cookies = mutableListOf<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            this.cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            this.cookies.add(cookie)
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return cookies.filter { it.expiresAt > now && it.matches(url) }
    }
}
