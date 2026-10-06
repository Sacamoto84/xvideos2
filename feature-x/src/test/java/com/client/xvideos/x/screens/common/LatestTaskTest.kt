package com.client.xvideos.x.screens.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Область задач — как у ScreenModel: задача стартует сразу, а продолжение после
 * смены потока приходит позже, отдельным шагом планировщика.
 *
 * Планировщик свой, без `runTest`: тот собирает необработанные исключения всех
 * корутин процесса, и тест падал бы из-за задач, оставшихся от других классов.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LatestTaskTest {

    private val scheduler = TestCoroutineScheduler()
    private val scope = CoroutineScope(UnconfinedTestDispatcher(scheduler) + Job())
    private var loading = false
    private val tasks = LatestTask(scope) { loading = false }

    @Test
    fun `отменённая задача не сообщает о простое, пока идёт новая`() {
        val otherThread = StandardTestDispatcher(scheduler)
        val never = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()

        tasks.launch {
            loading = true
            withContext(otherThread) { never.await() }
        }
        scheduler.runCurrent()
        tasks.launch {
            loading = true
            second.await()
        }
        // Сюда доходит finally первой, отменённой задачи.
        scheduler.runCurrent()

        assertTrue(loading)

        second.complete(Unit)
        scheduler.runCurrent()
        assertFalse(loading)
        scope.cancel()
    }

    @Test
    fun `задача без приостановок сообщает о простое`() {
        tasks.launch { loading = true }
        scheduler.runCurrent()

        assertFalse(loading)
        scope.cancel()
    }
}
