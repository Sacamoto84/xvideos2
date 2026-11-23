package com.client.xvideos.r.auth

import com.client.xvideos.r.network.http.isRedgifsApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class RAccountCredentialsTest {
    @Test
    fun `reads account name and token expiry`() {
        val token = tokenWithClaims("""{"iss":"https://auth2.redgifs.com","sub":"account","exp":2000000000}""")

        assertEquals(RAccountStatus.SignedIn("example", 2_000_000_000_000L), RAccountCredentials(token, "example").profile())
    }

    @Test
    fun `rejects a different issuer and malformed credentials`() {
        val wrongIssuer = tokenWithClaims("""{"iss":"https://example.com","sub":"account","exp":2000000000}""")
        val invalidExpiry = tokenWithClaims("""{"iss":"https://auth2.redgifs.com","sub":"account","exp":9223372036854775807}""")

        assertNull(RAccountCredentials(wrongIssuer, "example").profile())
        assertNull(RAccountCredentials(invalidExpiry, "example").profile())
        assertNull(RAccountCredentials("invalid", "example").profile())
    }

    @Test
    fun `only the HTTPS API origin receives account tokens`() {
        assertTrue("https://api.redgifs.com/v2/gifs".toHttpUrl().isRedgifsApi())
        assertFalse("http://api.redgifs.com/v2/gifs".toHttpUrl().isRedgifsApi())
        assertFalse("https://api.redgifs.com:444/v2/gifs".toHttpUrl().isRedgifsApi())
        assertFalse("https://api.redgifs.com.example.com/v2/gifs".toHttpUrl().isRedgifsApi())
        assertFalse("https://example.com@api.redgifs.com/v2/gifs".toHttpUrl().isRedgifsApi())
    }

    private fun tokenWithClaims(json: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.encodeToByteArray())
        return "header.$payload.signature"
    }
}
