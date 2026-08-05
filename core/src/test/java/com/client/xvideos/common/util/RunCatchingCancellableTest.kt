package com.client.xvideos.common.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class RunCatchingCancellableTest {

    @Test
    fun `успех оборачивается в Result success`() {
        val result = runCatchingCancellable { 42 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `обычная ошибка становится Result failure`() {
        val result = runCatchingCancellable { throw IOException("нет сети") }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `отмена пробрасывается наружу, а не превращается в failure`() {
        try {
            runCatchingCancellable { throw CancellationException("scope отменён") }
            fail("CancellationException должна была улететь наружу")
        } catch (e: CancellationException) {
            assertEquals("scope отменён", e.message)
        }
    }

    /**
     * Тот самый сценарий, ради которого хелпер и нужен: цикл загрузки после
     * отмены обязан остановиться, а не молоть остаток списка.
     */
    @Test
    fun `цикл загрузки прерывается на отмене`() = runBlocking {
        var attempts = 0

        val job = async {
            repeat(100) {
                runCatchingCancellable {
                    attempts++
                    delay(20)
                }
            }
        }

        delay(30)
        job.cancel()
        runCatching { job.await() }

        // Без проброса отмены цикл добежал бы до всех 100 итераций.
        assertTrue("итераций после отмены: $attempts", attempts < 100)
    }
}
