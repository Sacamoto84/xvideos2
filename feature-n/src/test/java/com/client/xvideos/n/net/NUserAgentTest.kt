package com.client.xvideos.n.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NUserAgentTest {

    @Before
    fun setUp() {
        NUserAgent.resetCacheForTest()
    }

    @Test
    fun `fallback user agent is non blank and valid format`() {
        val ua = NUserAgent.current
        assertNotNull(ua)
        assertTrue(ua.startsWith("Mozilla/5.0"))
        assertTrue(ua.contains("Android"))
    }

    @Test
    fun `cleanUserAgent does not strip webview indicators to prevent turnstile spoofing detection`() {
        val sampleUa = "Mozilla/5.0 (Linux; U; Android 10; Pixel 4; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.0.0 Mobile Safari/537.36"
        val cleaned = NUserAgent.cleanUserAgent(sampleUa)
        assertEquals(sampleUa, cleaned)
        assertTrue(cleaned.contains("; wv"))
        assertTrue(cleaned.contains("Version/4.0"))
    }
}
