package com.client.xvideos.n.net

import com.client.xvideos.n.repository.parseNFavoritesPage
import com.client.xvideos.n.repository.NRepository
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NResponseClassificationTest {

    @Test
    fun `redirect guard accepts only the canonical HTTPS origin`() {
        val trusted = Request.Builder().url("https://nhentai.net/login/").build()
        assertSame(trusted, requireTrustedNRequest(trusted))

        for (url in listOf("https://example.test/", "http://nhentai.net/", "https://nhentai.net:444/")) {
            val redirected = trusted.newBuilder().url(url).build()
            assertThrows(IOException::class.java) { requireTrustedNRequest(redirected) }
        }
    }

    @Test
    fun `server favorites cannot be changed through the web session fallback`() = runTest {
        val result = NRepository().setServerFavorite(1, favorite = false, apiKey = "")
        assertTrue(result.isFailure)
    }

    @Test
    fun `text in a successful response is not a challenge`() {
        val body = """{"result":[{"english_title":"Just a moment..."}],"num_pages":1}"""
        assertFalse(isNCloudflareChallenge(200, "application/json", null, body))
        assertFalse(isNCloudflareChallenge(403, "application/json", null, body))
    }

    @Test
    fun `challenge header and blocked HTML are recognized`() {
        assertTrue(isNCloudflareChallenge(429, "text/html", "challenge", ""))
        val html = """<!DOCTYPE html><html><head><title>Just a moment...</title></head></html>"""
        assertTrue(isNCloudflareChallenge(403, "text/html; charset=utf-8", null, html))
        assertFalse(isNCloudflareChallenge(200, "text/html", null, html))
    }

    @Test
    fun `favorites page stays valid when empty and login redirect is rejected`() {
        val emptyHtml = "<html><main></main></html>"
        val favoritesUrl = "https://nhentai.net/favorites/?page=1"
        val loginHtml = """<form action="/login/"><input type="password"></form>"""
        assertTrue(parseNFavoritesPage(favoritesUrl, emptyHtml, 1)?.items?.isEmpty() == true)
        assertNull(parseNFavoritesPage("https://nhentai.net/login/?next=/favorites/", loginHtml, 1))
        assertNull(parseNFavoritesPage(favoritesUrl, loginHtml, 1))
    }
}
