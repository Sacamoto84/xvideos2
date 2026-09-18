package com.client.xvideos.common.applock

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLockTimeoutTest {

    @Test
    fun `fromSeconds корректно находит существующие значения`() {
        assertEquals(AppLockTimeout.IMMEDIATELY, AppLockTimeout.fromSeconds(0))
        assertEquals(AppLockTimeout.SECONDS_30, AppLockTimeout.fromSeconds(30))
        assertEquals(AppLockTimeout.MINUTES_1, AppLockTimeout.fromSeconds(60))
        assertEquals(AppLockTimeout.MINUTES_5, AppLockTimeout.fromSeconds(300))
        assertEquals(AppLockTimeout.NEVER, AppLockTimeout.fromSeconds(-1))
    }

    @Test
    fun `fromSeconds возвращает значение по умолчанию для неизвестных секунд`() {
        assertEquals(AppLockTimeout.DEFAULT, AppLockTimeout.fromSeconds(999))
        assertEquals(AppLockTimeout.DEFAULT, AppLockTimeout.fromSeconds(-100))
        assertEquals(AppLockTimeout.MINUTES_1, AppLockTimeout.DEFAULT)
    }

    @Test
    fun `durationMillis возвращает корректные значения`() {
        assertEquals(0L, AppLockTimeout.IMMEDIATELY.durationMillis)
        assertEquals(30_000L, AppLockTimeout.SECONDS_30.durationMillis)
        assertEquals(60_000L, AppLockTimeout.MINUTES_1.durationMillis)
        assertEquals(300_000L, AppLockTimeout.MINUTES_5.durationMillis)
        assertEquals(0L, AppLockTimeout.NEVER.durationMillis)
    }
}
