package com.client.xvideos.common.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Ради чего заведён [launchCatching]: отказ сети не должен уносить процесс.
 * Проверяем обе стороны — исключение гасится, отмена проходит насквозь.
 */
class LaunchCatchingTest {

    @Test
    fun `отказ внутри не отменяет родительскую задачу`() = runTest {
        val parent = SupervisorJob()
        val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))

        val job = scope.launchCatching(message = "тест") {
            throw IOException("Unable to resolve host")
        }
        job.join()

        assertTrue("корутина обязана завершиться, а не упасть", job.isCompleted)
        assertFalse("родитель не должен быть отменён", parent.isCancelled)
    }

    @Test
    fun `значение отдаётся, когда отказа нет`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        var done = 0

        scope.launchCatching(message = "тест") { done = 42 }.join()

        assertEquals(42, done)
    }

    /**
     * На CancellationException держится отмена корутин. Проглотить её — сломать
     * уход с экрана: запросы продолжали бы жить после закрытия.
     */
    @Test
    fun `отмена проходит насквозь`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        val job: Job = scope.launchCatching(message = "тест") {
            throw CancellationException("уходим с экрана")
        }
        job.join()

        assertTrue("отмена обязана оставить задачу отменённой", job.isCancelled)
    }
}
