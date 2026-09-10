package com.client.xvideos.common.storage

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageCleanupGateTest {

    @Test
    fun `await не блокирует, если уборка не запускалась`() = runTest {
        val gate = StorageCleanupGate()

        gate.await()
        // Дошли сюда — значит await вернулся. Так ведёт себя процесс, в котором
        // App.onCreate ещё не успел стартовать уборку (unit-тесты, Preview).
        assertTrue(true)
    }

    @Test
    fun `await ждёт запущенную уборку и не запускает её дважды`() = runTest {
        val gate = StorageCleanupGate()
        val started = CompletableDeferred<Unit>()
        var runs = 0

        gate.start(this) {
            runs++
            started.complete(Unit)
        }
        gate.start(this) { runs++ }

        gate.await()
        started.await()

        assertEquals("повторный start обязан быть проигнорирован", 1, runs)
    }

    @Test
    fun `await переживает падение уборки`() = runTest {
        val gate = StorageCleanupGate()
        gate.start(this) { error("уборка упала") }

        gate.await()
        // Падение уборки не должно превращаться в падение ожидающего: он ждёт
        // «уборка больше не идёт», а не «уборка удалась».
        assertTrue(true)
    }
}
