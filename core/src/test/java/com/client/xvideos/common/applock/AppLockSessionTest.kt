package com.client.xvideos.common.applock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockSessionTest {

    @Before
    fun setUp() {
        AppLockSession.lock()
    }

    @Test
    fun `при возврате из фона до истечения таймаута сессия остаётся разблокированной`() {
        AppLockSession.unlock()
        assertTrue(AppLockSession.isUnlocked())

        val backgroundTime = 10_000L
        AppLockSession.onAppBackgrounded(elapsedNow = backgroundTime)

        // Прошло 20 секунд (таймаут 30 секунд)
        val locked = AppLockSession.onAppForegrounded(
            timeoutSeconds = AppLockTimeout.SECONDS_30.seconds,
            isLockConfigured = true,
            elapsedNow = backgroundTime + 20_000L
        )

        assertFalse(locked)
        assertTrue(AppLockSession.isUnlocked())
        assertEquals(0L, AppLockSession.lastBackgroundTime())
    }

    @Test
    fun `при возврате из фона после истечения таймаута сессия блокируется`() {
        AppLockSession.unlock()
        assertTrue(AppLockSession.isUnlocked())

        val backgroundTime = 10_000L
        AppLockSession.onAppBackgrounded(elapsedNow = backgroundTime)

        // Прошло 30 секунд (ровно порог)
        val locked = AppLockSession.onAppForegrounded(
            timeoutSeconds = AppLockTimeout.SECONDS_30.seconds,
            isLockConfigured = true,
            elapsedNow = backgroundTime + 30_000L
        )

        assertTrue(locked)
        assertFalse(AppLockSession.isUnlocked())
        assertEquals(0L, AppLockSession.lastBackgroundTime())
    }

    @Test
    fun `при таймауте Сразу сессия блокируется сразу же при возврате`() {
        AppLockSession.unlock()
        assertTrue(AppLockSession.isUnlocked())

        val backgroundTime = 10_000L
        AppLockSession.onAppBackgrounded(elapsedNow = backgroundTime)

        val locked = AppLockSession.onAppForegrounded(
            timeoutSeconds = AppLockTimeout.IMMEDIATELY.seconds,
            isLockConfigured = true,
            elapsedNow = backgroundTime + 100L
        )

        assertTrue(locked)
        assertFalse(AppLockSession.isUnlocked())
    }

    @Test
    fun `при таймауте Никогда сессия не блокируется даже через большой интервал`() {
        AppLockSession.unlock()
        assertTrue(AppLockSession.isUnlocked())

        val backgroundTime = 10_000L
        AppLockSession.onAppBackgrounded(elapsedNow = backgroundTime)

        // Прошёл 1 час
        val locked = AppLockSession.onAppForegrounded(
            timeoutSeconds = AppLockTimeout.NEVER.seconds,
            isLockConfigured = true,
            elapsedNow = backgroundTime + 3_600_000L
        )

        assertFalse(locked)
        assertTrue(AppLockSession.isUnlocked())
    }

    @Test
    fun `если блокировка приложения не настроена, сессия не блокируется`() {
        AppLockSession.unlock()
        assertTrue(AppLockSession.isUnlocked())

        val backgroundTime = 10_000L
        AppLockSession.onAppBackgrounded(elapsedNow = backgroundTime)

        val locked = AppLockSession.onAppForegrounded(
            timeoutSeconds = AppLockTimeout.SECONDS_30.seconds,
            isLockConfigured = false,
            elapsedNow = backgroundTime + 60_000L
        )

        assertFalse(locked)
        assertTrue(AppLockSession.isUnlocked())
    }

    @Test
    fun `unlock и lock сбрасывают время ухода в фон`() {
        AppLockSession.onAppBackgrounded(elapsedNow = 5_000L)
        assertEquals(5_000L, AppLockSession.lastBackgroundTime())

        AppLockSession.unlock()
        assertEquals(0L, AppLockSession.lastBackgroundTime())

        AppLockSession.onAppBackgrounded(elapsedNow = 8_000L)
        assertEquals(8_000L, AppLockSession.lastBackgroundTime())

        AppLockSession.lock()
        assertEquals(0L, AppLockSession.lastBackgroundTime())
    }
}
