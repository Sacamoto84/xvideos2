package com.client.xvideos.n.net

import okhttp3.Headers
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class NRateLimit(
    private val clock: TimeSource = TimeSource.Monotonic,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private var retryAt: TimeMark? = null

    @Synchronized
    fun onRateLimited(headers: Headers): NNetworkResult.Error {
        val retryAfter = headers["Retry-After"]?.toLongOrNull()?.seconds
            ?: headers.getDate("Retry-After")?.let { (it.time - nowMillis()).milliseconds }
        if (retryAfter != null && retryAfter.isFinite() && retryAfter > remaining()) {
            retryAt = clock.markNow() + retryAfter
        }
        return error(remaining())
    }

    @Synchronized
    fun blockedRequest(): NNetworkResult.Error? =
        remaining().takeIf { it > Duration.ZERO }?.let(::error)

    private fun remaining(): Duration =
        retryAt?.let { (-it.elapsedNow()).coerceAtLeast(Duration.ZERO) } ?: Duration.ZERO

    private fun error(remaining: Duration): NNetworkResult.Error {
        val seconds = remaining.inWholeSeconds
        val roundedUp = if (remaining > seconds.seconds) seconds + 1 else seconds
        val message = if (roundedUp > 0) {
            "Слишком много запросов. Повторите через $roundedUp сек."
        } else {
            "Слишком много запросов. Повторите позже."
        }
        return NNetworkResult.Error(429, message)
    }
}
