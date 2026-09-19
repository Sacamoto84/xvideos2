package com.client.xvideos.common.applock

import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockLifecycleObserverTest {

    private open class FakeContext : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    private val fakeContext = FakeContext()

    private val fakeOwner = object : LifecycleOwner {
        override val lifecycle: Lifecycle
            get() = throw UnsupportedOperationException("Not needed for observer callbacks")
    }

    @Before
    fun setUp() {
        AppLockSession.lock()
    }

    @Test
    fun `onStop and onStart do not throw when Settings is not initialized`() {
        val observer = AppLockLifecycleObserver(
            context = fakeContext,
            elapsedRealtimeProvider = { 10_000L }
        )
        // Should safely execute without throwing UninitializedPropertyAccessException or unmocked SystemClock
        observer.onStop(fakeOwner)
        observer.onStart(fakeOwner)
        assertFalse(AppLockSession.isUnlocked())
    }

    @Test
    fun `onStop does not change session if lock is not enabled`() {
        AppLockSession.unlock()
        assertTrue(AppLockSession.isUnlocked())

        val observer = AppLockLifecycleObserver(
            context = fakeContext,
            elapsedRealtimeProvider = { 10_000L }
        )
        observer.onStop(fakeOwner)

        // Lock is not enabled so onStop returns early without recording background
        assertTrue(AppLockSession.isUnlocked())
        assertEquals(0L, AppLockSession.lastBackgroundTime())
    }
}
