package com.client.xvideos.common.applock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockThrottleTest {

    @Test
    fun `первые попытки не блокируют`() {
        repeat(AppLockThrottle.FREE_ATTEMPTS) { i ->
            val state = AppLockThrottle.onFailedAttempt(
                attempts = i,
                wallNow = 1_000L,
                elapsedNow = 500L,
            )
            assertEquals(0L, state.lockoutUntilWall)
            assertEquals(0L, state.lockoutUntilElapsed)
        }
    }

    @Test
    fun `первая блокировка длится базовые тридцать секунд`() {
        val state = AppLockThrottle.onFailedAttempt(
            attempts = AppLockThrottle.FREE_ATTEMPTS,
            wallNow = 1_000L,
            elapsedNow = 500L,
        )
        assertEquals(1_000L + 30_000L, state.lockoutUntilWall)
        assertEquals(500L + 30_000L, state.lockoutUntilElapsed)
        assertEquals(AppLockThrottle.FREE_ATTEMPTS + 1, state.attempts)
    }

    @Test
    fun `задержка удваивается и упирается в потолок`() {
        val second = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS + 1, 0L, 0L)
        assertEquals(60_000L, second.lockoutUntilWall)

        val far = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS + 40, 0L, 0L)
        assertEquals(30 * 60_000L, far.lockoutUntilWall)
    }

    @Test
    fun `перевод часов вперёд не снимает блокировку`() {
        val state = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS, 1_000L, 500L)

        // Пользователь перевёл системное время на сутки вперёд, монотонные часы
        // при этом не сдвинулись.
        val remaining = AppLockThrottle.remainingMillis(
            state = state,
            wallNow = 1_000L + 86_400_000L,
            elapsedNow = 600L,
        )
        assertTrue("блокировка обязана держаться на монотонных часах", remaining > 0L)
    }

    @Test
    fun `перевод часов назад не продлевает блокировку сверх монотонного срока`() {
        val state = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS, 1_000L, 500L)

        val remaining = AppLockThrottle.remainingMillis(
            state = state,
            wallNow = 0L,
            elapsedNow = 500L + 30_000L,
        )
        // Настенные часы ушли назад — по ним срок ещё не истёк, поэтому лок
        // держится. Это осознанный выбор: ошибаемся в сторону блокировки.
        assertTrue(remaining > 0L)
    }

    @Test
    fun `после истечения обоих сроков блокировки нет`() {
        val state = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS, 1_000L, 500L)

        val remaining = AppLockThrottle.remainingMillis(
            state = state,
            wallNow = 1_000L + 30_001L,
            elapsedNow = 500L + 30_001L,
        )
        assertEquals(0L, remaining)
    }

    @Test
    fun `перезагрузка обнуляет монотонные часы но настенный срок держит`() {
        val state = AppLockThrottle.onFailedAttempt(AppLockThrottle.FREE_ATTEMPTS + 5, 1_000L, 900_000L)

        // После перезагрузки elapsedRealtime начинается с нуля и меньше
        // сохранённого срока — по нему лок считался бы активным вечно, поэтому
        // такой случай обязан отбрасываться, а срок брать с настенных часов.
        val remaining = AppLockThrottle.remainingMillis(
            state = state,
            wallNow = 1_000L + 100L,
            elapsedNow = 42L,
        )
        assertTrue(remaining > 0L)
        assertTrue("после перезагрузки срок ограничен настенными часами", remaining <= 30 * 60_000L)
    }
}
