package com.client.xvideos.n.net

import org.junit.Assert.assertEquals
import org.junit.Test

class NCookieHelperTest {

    @Test
    fun `extractCookieValue extracts existing cookie`() {
        val cookies = "csrftoken=abc123xyz; sessionid=my_secret_session_456; cf_clearance=cf_789"
        assertEquals("my_secret_session_456", NCookieHelper.extractCookieValue(cookies, "sessionid"))
        assertEquals("cf_789", NCookieHelper.extractCookieValue(cookies, "cf_clearance"))
        assertEquals("abc123xyz", NCookieHelper.extractCookieValue(cookies, "csrftoken"))
    }

    @Test
    fun `extractCookieValue returns empty for missing cookie`() {
        val cookies = "csrftoken=abc123xyz; other=1"
        assertEquals("", NCookieHelper.extractCookieValue(cookies, "sessionid"))
    }

    @Test
    fun `extractCookieValue handles trailing spaces and multiple values`() {
        val cookies = "  sessionid=test_val ; other=2 "
        assertEquals("test_val", NCookieHelper.extractCookieValue(cookies, "sessionid"))
    }
}
