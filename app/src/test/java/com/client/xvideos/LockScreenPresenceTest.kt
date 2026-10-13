package com.client.xvideos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LockScreenPresenceTest {

    private val task = 7

    @Before
    fun setUp() {
        LockScreenPresence.resetForTesting()
    }

    @Test
    fun `из запросов подряд замок запускает только первый`() {
        // onStart, onResume и событие «сеанс закрыли» приходят вплотную.
        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_000L))
        assertFalse(LockScreenPresence.claim(task, elapsedNow = 1_001L))
        assertFalse(LockScreenPresence.claim(task, elapsedNow = 1_300L))
    }

    @Test
    fun `пока экран замка показан, новый не запускается`() {
        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_000L))
        LockScreenPresence.onShown(task)

        // Срок ожидания запуска давно вышел, но экран на месте.
        assertFalse(LockScreenPresence.claim(task, elapsedNow = 1_000L + 10 * LockScreenPresence.START_GRACE_MS))
    }

    @Test
    fun `после закрытия экрана замка следующий запускается сразу`() {
        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_000L))
        LockScreenPresence.onShown(task)
        LockScreenPresence.onGone(task)

        // Код ввели, и сеанс тут же закрыли снова — ждать нечего.
        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_050L))
    }

    @Test
    fun `несостоявшийся запуск не оставляет приложение без замка`() {
        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_000L))

        // Экран так и не создался: по истечении срока запрос повторяют.
        assertFalse(LockScreenPresence.claim(task, elapsedNow = 1_000L + LockScreenPresence.START_GRACE_MS - 1))
        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_000L + LockScreenPresence.START_GRACE_MS))
    }

    @Test
    fun `экран, ушедший сразу после создания, запрос освобождает`() {
        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_000L))
        LockScreenPresence.onRequestDropped(task)

        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_010L))
    }

    @Test
    fun `вторая задача показывает свой замок независимо от первой`() {
        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_000L))
        LockScreenPresence.onShown(task)

        assertTrue(LockScreenPresence.claim(task + 1, elapsedNow = 1_001L))
    }

    @Test
    fun `лишнее снятие с учёта счётчик не портит`() {
        LockScreenPresence.onGone(task)

        assertTrue(LockScreenPresence.claim(task, elapsedNow = 1_000L))
        LockScreenPresence.onShown(task)
        assertFalse(LockScreenPresence.claim(task, elapsedNow = 1_001L))
    }
}
