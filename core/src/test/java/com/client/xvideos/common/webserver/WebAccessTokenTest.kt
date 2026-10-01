package com.client.xvideos.common.webserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAccessTokenTest {

    @Test
    fun `токен случайный и пригоден для URL и cookie`() {
        val first = WebAccessToken.generate()
        val second = WebAccessToken.generate()

        assertNotEquals(first, second)
        assertEquals(43, first.length) // 32 байта в base64url без паддинга
        assertTrue(first.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun `совпадает только тот же токен`() {
        val token = WebAccessToken.generate()

        assertTrue(WebAccessToken.matches(token, token))
        assertFalse(WebAccessToken.matches(WebAccessToken.generate(), token))
        assertFalse(WebAccessToken.matches(token.dropLast(1), token))
    }

    @Test
    fun `пустой или отсутствующий токен не пускает`() {
        assertFalse(WebAccessToken.matches(null, "abc"))
        assertFalse(WebAccessToken.matches("", "abc"))
        assertFalse(WebAccessToken.matches("abc", null))
        assertFalse(WebAccessToken.matches("", ""))
    }

    @Test
    fun `ссылка содержит токен, пустой адрес остаётся пустым`() {
        assertEquals("http://192.168.1.5:8080/?t=abc", WebAccessToken.accessUrl("http://192.168.1.5:8080", "abc"))
        assertEquals("", WebAccessToken.accessUrl("", "abc"))
    }
}
