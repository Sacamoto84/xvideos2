package com.client.xvideos.common.applock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Когда окну нужен `FLAG_SECURE`.
 *
 * При включённом замке и выключенном «размытии в недавних» флаг не ставился,
 * пока сеанс разблокирован: система снимала превью с содержимым, и после
 * срабатывания замка карточка в «Недавних» показывала его без ввода кода.
 */
class WindowPrivacyTest {

    @Test
    fun `свёрнутое приложение с включённым замком закрывает превью и без размытия`() {
        assertTrue(
            WindowPrivacy.needsSecureFlag(
                isLockShown = false, isMinimized = true, blurRecentTasks = false, lockEnabled = true,
            )
        )
    }

    @Test
    fun `показанный замок закрывает окно всегда`() {
        assertTrue(
            WindowPrivacy.needsSecureFlag(
                isLockShown = true, isMinimized = false, blurRecentTasks = false, lockEnabled = true,
            )
        )
    }

    @Test
    fun `свёрнутое приложение с размытием закрывает превью и без замка`() {
        assertTrue(
            WindowPrivacy.needsSecureFlag(
                isLockShown = false, isMinimized = true, blurRecentTasks = true, lockEnabled = false,
            )
        )
    }

    @Test
    fun `открытое разблокированное приложение скриншоты не запрещает`() {
        assertFalse(
            WindowPrivacy.needsSecureFlag(
                isLockShown = false, isMinimized = false, blurRecentTasks = true, lockEnabled = true,
            )
        )
    }

    @Test
    fun `без замка и без размытия свёрнутое приложение окно не закрывает`() {
        assertFalse(
            WindowPrivacy.needsSecureFlag(
                isLockShown = false, isMinimized = true, blurRecentTasks = false, lockEnabled = false,
            )
        )
    }

    @Test
    fun `камуфляж калькулятора не ставит FLAG_SECURE для естественной миниатюры в недавних`() {
        assertFalse(
            WindowPrivacy.needsSecureFlag(
                isLockShown = true,
                isMinimized = false,
                blurRecentTasks = false,
                lockEnabled = true,
                isCamouflage = true,
            )
        )
    }
}
