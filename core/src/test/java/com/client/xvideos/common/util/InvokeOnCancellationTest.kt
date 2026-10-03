package com.client.xvideos.common.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * [invokeOnCancellation] обязан сработать, пока тело корутины ещё заперто в
 * блокирующем вызове: ради этого он и нужен вместо `Job.invokeOnCompletion`.
 */
class InvokeOnCancellationTest {

    @Test
    fun `хук срабатывает при отмене, пока тело заблокировано`() = runBlocking {
        val bodyBlocked = CountDownLatch(1)
        val hookCalled = CountDownLatch(1)
        var unblockedByHook = false

        // Один поток на всю корутину: наблюдателю негде выполниться, кроме потока отменяющего.
        val job = launch(Dispatchers.IO.limitedParallelism(1)) {
            val handle = invokeOnCancellation { hookCalled.countDown() }
            try {
                bodyBlocked.countDown()
                unblockedByHook = hookCalled.await(5, TimeUnit.SECONDS)
            } finally {
                handle.dispose()
            }
        }

        assertTrue(bodyBlocked.await(5, TimeUnit.SECONDS))
        job.cancelAndJoin()

        assertTrue("хук обязан разбудить блокирующее тело", unblockedByHook)
    }

    @Test
    fun `после dispose хук не вызывается`() = runBlocking {
        val calls = AtomicInteger()
        val registered = CountDownLatch(1)
        val release = CountDownLatch(1)

        val job = launch(Dispatchers.IO) {
            val handle = invokeOnCancellation { calls.incrementAndGet() }
            handle.dispose()
            registered.countDown()
            release.await(5, TimeUnit.SECONDS)
        }

        assertTrue(registered.await(5, TimeUnit.SECONDS))
        job.cancel()
        release.countDown()
        job.join()

        assertEquals(0, calls.get())
    }

    @Test
    fun `при обычном завершении хук не вызывается`() = runBlocking {
        val calls = AtomicInteger()

        coroutineScope {
            val handle = invokeOnCancellation { calls.incrementAndGet() }
            handle.dispose()
        }

        assertEquals(0, calls.get())
    }
}
