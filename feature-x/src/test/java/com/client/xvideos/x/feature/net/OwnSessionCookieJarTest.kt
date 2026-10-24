package com.client.xvideos.x.feature.net

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cookie запроса со своей сессией живут только в своём хранилище. */
class OwnSessionCookieJarTest {

    private val site = "https://www.example.test/".toHttpUrl()

    private fun cookie(value: String) = Cookie.parse(site, value)!!

    @Test
    fun `cookie из ответа уходят в следующий запрос того же сайта`() {
        val jar = OwnSessionCookieJar()
        jar.saveFromResponse(site, listOf(cookie("session_token=abc; Path=/")))

        val sent = jar.loadForRequest("https://www.example.test/gay".toHttpUrl())

        assertEquals(listOf("abc"), sent.map { it.value })
    }

    @Test
    fun `чужой сайт cookie не получает`() {
        val jar = OwnSessionCookieJar()
        jar.saveFromResponse(site, listOf(cookie("session_token=abc; Path=/")))

        assertTrue(jar.loadForRequest("https://other.test/".toHttpUrl()).isEmpty())
    }

    @Test
    fun `у двух экземпляров свои cookie`() {
        val first = OwnSessionCookieJar()
        val second = OwnSessionCookieJar()
        first.saveFromResponse(site, listOf(cookie("session_token=abc; Path=/")))

        assertTrue(second.loadForRequest(site).isEmpty())
    }

    @Test
    fun `cookie с Max-Age=0 не отдаётся`() {
        val jar = OwnSessionCookieJar()
        jar.saveFromResponse(site, listOf(cookie("session_token=abc; Max-Age=0; Path=/")))

        assertTrue(jar.loadForRequest(site).isEmpty())
    }

    @Test
    fun `cookie с тем же именем заменяется`() {
        val jar = OwnSessionCookieJar()
        jar.saveFromResponse(site, listOf(cookie("session_token=abc; Path=/")))
        jar.saveFromResponse(site, listOf(cookie("session_token=def; Path=/")))

        assertEquals(listOf("def"), jar.loadForRequest(site).map { it.value })
    }
}
