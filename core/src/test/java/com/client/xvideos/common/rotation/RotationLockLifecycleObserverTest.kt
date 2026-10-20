package com.client.xvideos.common.rotation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationLockLifecycleObserverTest {

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

    private val owner = object : LifecycleOwner {
        override val lifecycle: Lifecycle
            get() = throw UnsupportedOperationException("Not needed for observer callbacks")
    }

    @Test
    fun `lock enabled at start and lifted during use is restored on stop`() {
        val settings = FakeRotationSettings(autoRotate = false, rotation = 0)
        val observer = RotationLockLifecycleObserver(settings)

        observer.onStart(owner)
        settings.autoRotate = true
        settings.rotation = 1
        observer.onStop(owner)

        assertEquals(listOf(0), settings.locks)
    }

    @Test
    fun `lock still enabled on stop is left alone`() {
        val settings = FakeRotationSettings(autoRotate = false, rotation = 0)
        val observer = RotationLockLifecycleObserver(settings)

        observer.onStart(owner)
        observer.onStop(owner)

        assertTrue(settings.locks.isEmpty())
    }

    @Test
    fun `auto-rotate on at start stays on`() {
        val settings = FakeRotationSettings(autoRotate = true)
        val observer = RotationLockLifecycleObserver(settings)

        observer.onStart(owner)
        observer.onStop(owner)

        assertTrue(settings.locks.isEmpty())
    }

    @Test
    fun `without write permission nothing is written`() {
        val settings = FakeRotationSettings(autoRotate = false, writable = false)
        val observer = RotationLockLifecycleObserver(settings)

        observer.onStart(owner)
        settings.autoRotate = true
        observer.onStop(owner)

        assertTrue(settings.locks.isEmpty())
    }

    @Test
    fun `each foreground session reads the lock again`() {
        val settings = FakeRotationSettings(autoRotate = false)
        val observer = RotationLockLifecycleObserver(settings)

        observer.onStart(owner)
        observer.onStop(owner)
        // Между сессиями блокировку сняли вне приложения: следующая сессия её не возвращает.
        settings.autoRotate = true
        observer.onStart(owner)
        observer.onStop(owner)

        assertTrue(settings.locks.isEmpty())
    }

    @Test
    fun `stop without start does nothing`() {
        val settings = FakeRotationSettings(autoRotate = true)
        val observer = RotationLockLifecycleObserver(settings)

        observer.onStop(owner)

        assertTrue(settings.locks.isEmpty())
    }
}
