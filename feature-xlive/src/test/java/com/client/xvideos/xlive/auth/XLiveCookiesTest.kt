package com.client.xvideos.xlive.auth

import com.client.xvideos.xlive.xLiveUrlStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XLiveCookiesTest {

    @Test
    fun `значение cookie из заголовка — по точному имени`() {
        val header = "csrftoken=abc; sessionid=s1; sessionid_old=x"

        assertEquals("abc", cookieValue(header, "csrftoken"))
        assertEquals("s1", cookieValue(header, "sessionid"))
    }

    @Test
    fun `нет cookie или она пустая — null`() {
        assertNull(cookieValue("csrftoken=abc", "sessionid"))
        assertNull(cookieValue("sessionid=; csrftoken=abc", "sessionid"))
        assertNull(cookieValue("", "sessionid"))
    }

    @Test
    fun `хост сайта — сам сайт и поддомены, чужие нет`() {
        val domain = xLiveUrlStart.removePrefix("https://www.")

        assertTrue(isXLiveHost("www.$domain"))
        assertTrue(isXLiveHost(domain))
        assertFalse(isXLiveHost("other.test"))
        assertFalse(isXLiveHost("evil$domain"))
        assertFalse(isXLiveHost(""))
    }

    @Test
    fun `ушли со страниц входа — страница сайта и есть сессия`() {
        assertTrue(isPastSignInPages("$xLiveUrlStart/", hasSession = true))
        assertTrue(isPastSignInPages("$xLiveUrlStart/female-cams/", hasSession = true))
    }

    @Test
    fun `ещё на шагах входа — без сессии, на служебной странице или на чужом сайте`() {
        assertFalse(isPastSignInPages("$xLiveUrlStart/", hasSession = false))
        assertFalse(isPastSignInPages(X_LIVE_SIGN_IN_URL, hasSession = true))
        assertFalse(isPastSignInPages("$xLiveUrlStart/auth/login/?next=/", hasSession = true))
        assertFalse(isPastSignInPages("$xLiveUrlStart/security/tfa/auth/", hasSession = true))
        assertFalse(isPastSignInPages("$xLiveUrlStart/auth-sso/logged-in/", hasSession = true))
        assertFalse(isPastSignInPages("$xLiveUrlStart/accounts/age_verification/", hasSession = true))
        assertFalse(isPastSignInPages("https://other.test/", hasSession = true))
        assertFalse(isPastSignInPages("не адрес", hasSession = true))
    }

    @Test
    fun `шаг входа для журнала — служебный раздел, сайт или другой сайт, без остального пути`() {
        assertEquals("/security/", signInStep("$xLiveUrlStart/security/tfa/auth/?next=/"))
        assertEquals("/auth/", signInStep(X_LIVE_SIGN_IN_URL))
        assertEquals("сайт", signInStep("$xLiveUrlStart/room_a/"))
        assertEquals("другой сайт", signInStep("https://other.test/oauth/authorize"))
    }
}
