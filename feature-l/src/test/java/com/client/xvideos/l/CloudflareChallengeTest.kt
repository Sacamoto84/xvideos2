package com.client.xvideos.l

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudflareChallengeTest {

    @Test
    fun `пассивный скрипт Cloudflare на странице 500 не считается проверкой`() {
        assertFalse(isCloudflareChallenge(cfMitigated = null, body = SERVER_ERROR_PAGE))
    }

    @Test
    fun `заголовок cf-mitigated challenge распознаётся как проверка`() {
        assertTrue(isCloudflareChallenge(cfMitigated = "challenge", body = "<html></html>"))
    }

    @Test
    fun `страница Just a moment распознаётся как проверка`() {
        assertTrue(isCloudflareChallenge(cfMitigated = null, body = CHALLENGE_PAGE))
    }

    @Test
    fun `пустой ответ не считается проверкой`() {
        assertFalse(isCloudflareChallenge(cfMitigated = null, body = ""))
    }

    companion object {
        /**
         * Ответ сервера L при отказе origin (2026-10-02): Cloudflare сам
         * подкладывает в любую HTML-страницу пассивный скрипт
         * `/cdn-cgi/challenge-platform/scripts/jsd/main.js`.
         */
        const val SERVER_ERROR_PAGE = """<html>
  <head>
    <title>Internal Server Error</title>
  </head>
  <body>
    <h1><p>Internal Server Error</p></h1>
  <script>(function(){var a=document.createElement('script');a.src='/cdn-cgi/challenge-platform/scripts/jsd/main.js';document.getElementsByTagName('head')[0].appendChild(a);})();</script></body>
</html>"""

        const val CHALLENGE_PAGE = """<!DOCTYPE html><html><head><title>Just a moment...</title></head>
<body><script>window._cf_chl_opt={cType:'managed'};</script></body></html>"""
    }
}
