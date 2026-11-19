package com.client.xvideos.n.net

import okhttp3.Headers
import okhttp3.Headers.Companion.headersOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class NRateLimitTest {
    @Test
    fun `a concurrent shorter retry does not shorten an existing cooldown`() {
        val clock = TestTimeSource()
        val rateLimit = NRateLimit(clock)
        assertEquals(429, rateLimit.onRateLimited(headersOf("Retry-After", "10")).code)

        clock += 2.seconds
        rateLimit.onRateLimited(headersOf("Retry-After", "3"))
        assertEquals("Слишком много запросов. Повторите через 8 сек.", rateLimit.blockedRequest()?.message)

        clock += 7999.milliseconds
        assertEquals("Слишком много запросов. Повторите через 1 сек.", rateLimit.blockedRequest()?.message)
        clock += 1.milliseconds
        assertNull(rateLimit.blockedRequest())
    }

    @Test
    fun `an HTTP date sets a cooldown and missing or expired dates do not`() {
        val now = Instant.parse("2026-11-19T12:00:00Z").toEpochMilli()
        val clock = TestTimeSource()
        val rateLimit = NRateLimit(clock) { now }

        assertEquals(429, rateLimit.onRateLimited(Headers.Builder().build()).code)
        assertNull(rateLimit.blockedRequest())
        rateLimit.onRateLimited(headersOf("Retry-After", "Thu, 19 Nov 2026 11:59:00 GMT"))
        assertNull(rateLimit.blockedRequest())

        rateLimit.onRateLimited(headersOf("Retry-After", "Thu, 19 Nov 2026 12:00:10 GMT"))
        assertEquals("Слишком много запросов. Повторите через 10 сек.", rateLimit.blockedRequest()?.message)
        clock += 10.seconds
        assertNull(rateLimit.blockedRequest())
    }
}
