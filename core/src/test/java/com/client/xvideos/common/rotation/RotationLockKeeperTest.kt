package com.client.xvideos.common.rotation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationLockKeeperTest {

    private class FakeRotationSettings(
        var autoRotate: Boolean,
        var rotation: Int = 0,
        var writable: Boolean = true,
    ) : RotationSettings {
        val locks = mutableListOf<Int>()

        override fun isAutoRotateOn(): Boolean = autoRotate
        override fun lockedRotation(): Int = rotation
        override fun canWrite(): Boolean = writable
        override fun lock(rotation: Int) {
            locks += rotation
            autoRotate = false
            this.rotation = rotation
        }
    }

    @Test
    fun `lock enabled on top and lifted during use is restored on leaving top`() {
        val settings = FakeRotationSettings(autoRotate = false, rotation = 0)
        val keeper = RotationLockKeeper(settings)

        keeper.onTop()
        settings.autoRotate = true
        settings.rotation = 1
        keeper.onLeftTop()

        assertEquals(listOf(0), settings.locks)
    }

    @Test
    fun `lock still enabled on leaving top is left alone`() {
        val settings = FakeRotationSettings(autoRotate = false, rotation = 0)
        val keeper = RotationLockKeeper(settings)

        keeper.onTop()
        keeper.onLeftTop()

        assertTrue(settings.locks.isEmpty())
    }

    @Test
    fun `auto-rotate on when app got on top stays on`() {
        val settings = FakeRotationSettings(autoRotate = true)
        val keeper = RotationLockKeeper(settings)

        keeper.onTop()
        keeper.onLeftTop()

        assertTrue(settings.locks.isEmpty())
    }

    @Test
    fun `without write permission nothing is written`() {
        val settings = FakeRotationSettings(autoRotate = false, writable = false)
        val keeper = RotationLockKeeper(settings)

        keeper.onTop()
        settings.autoRotate = true
        keeper.onLeftTop()

        assertTrue(settings.locks.isEmpty())
    }

    @Test
    fun `each time on top the lock is read again`() {
        val settings = FakeRotationSettings(autoRotate = false)
        val keeper = RotationLockKeeper(settings)

        keeper.onTop()
        keeper.onLeftTop()
        // Пока приложение было не наверху, блокировку сняли: её не возвращают.
        settings.autoRotate = true
        keeper.onTop()
        keeper.onLeftTop()

        assertTrue(settings.locks.isEmpty())
    }

    @Test
    fun `switch between own activities restores and remembers again`() {
        val settings = FakeRotationSettings(autoRotate = false, rotation = 0)
        val keeper = RotationLockKeeper(settings)

        keeper.onTop()
        settings.autoRotate = true
        // Прежняя верхняя Activity получает false, новая — true.
        keeper.onLeftTop()
        keeper.onTop()
        keeper.onLeftTop()

        assertEquals(listOf(0), settings.locks)
    }

    @Test
    fun `leaving top without being on top does nothing`() {
        val settings = FakeRotationSettings(autoRotate = true)
        val keeper = RotationLockKeeper(settings)

        keeper.onLeftTop()

        assertTrue(settings.locks.isEmpty())
    }
}
