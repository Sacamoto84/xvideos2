package com.client.xvideos.common.videoplayer.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class ExoPlayerLifecycleTest {

    private class FakeExoPlayerHolder {
        var playWhenReady: Boolean = true

        val player: ExoPlayer = Proxy.newProxyInstance(
            ExoPlayer::class.java.classLoader,
            arrayOf(ExoPlayer::class.java),
            InvocationHandler { _, method, args ->
                when (method.name) {
                    "setPlayWhenReady" -> {
                        playWhenReady = args[0] as Boolean
                        null
                    }
                    "getPlayWhenReady" -> playWhenReady
                    "toString" -> "FakeExoPlayer"
                    "hashCode" -> 1
                    "equals" -> false
                    else -> when (method.returnType) {
                        Boolean::class.javaPrimitiveType -> false
                        Int::class.javaPrimitiveType -> 0
                        Long::class.javaPrimitiveType -> 0L
                        Float::class.javaPrimitiveType -> 0f
                        Double::class.javaPrimitiveType -> 0.0
                        Void.TYPE -> null
                        else -> null
                    }
                }
            }
        ) as ExoPlayer
    }

    private val fakeLifecycleOwner = Proxy.newProxyInstance(
        LifecycleOwner::class.java.classLoader,
        arrayOf(LifecycleOwner::class.java),
        InvocationHandler { _, _, _ -> null }
    ) as LifecycleOwner

    @Test
    fun `onPause stops playback and onResume restores it when not paused`() {
        val holder = FakeExoPlayerHolder()
        holder.playWhenReady = true

        var isPause = false
        val observer = getExoPlayerLifecycleObserver(
            exoPlayer = holder.player,
            isPause = { isPause }
        )

        // Приложение свернули
        observer.onStateChanged(fakeLifecycleOwner, Lifecycle.Event.ON_PAUSE)
        assertFalse(holder.playWhenReady)

        // Приложение развернули, пользователь не ставил на паузу
        observer.onStateChanged(fakeLifecycleOwner, Lifecycle.Event.ON_RESUME)
        assertTrue(holder.playWhenReady)
    }

    @Test
    fun `onResume does not start playback if user paused video`() {
        val holder = FakeExoPlayerHolder()
        holder.playWhenReady = true

        var isPause = false
        val observer = getExoPlayerLifecycleObserver(
            exoPlayer = holder.player,
            isPause = { isPause }
        )

        // Сворачивание приложения
        observer.onStateChanged(fakeLifecycleOwner, Lifecycle.Event.ON_PAUSE)
        assertFalse(holder.playWhenReady)

        // Пользователь поставил на паузу во время или перед возвратом
        isPause = true
        observer.onStateChanged(fakeLifecycleOwner, Lifecycle.Event.ON_RESUME)
        assertFalse(holder.playWhenReady)
    }

    @Test
    fun `onStop marks background and onResume restores playback`() {
        val holder = FakeExoPlayerHolder()
        holder.playWhenReady = true

        val observer = getExoPlayerLifecycleObserver(
            exoPlayer = holder.player,
            isPause = { false }
        )

        observer.onStateChanged(fakeLifecycleOwner, Lifecycle.Event.ON_STOP)
        assertFalse(holder.playWhenReady)

        observer.onStateChanged(fakeLifecycleOwner, Lifecycle.Event.ON_RESUME)
        assertTrue(holder.playWhenReady)
    }

    @Test
    fun `multiple resumes without prior pause do not force playWhenReady`() {
        val holder = FakeExoPlayerHolder()
        holder.playWhenReady = false

        val observer = getExoPlayerLifecycleObserver(
            exoPlayer = holder.player,
            isPause = { false }
        )

        // ON_RESUME без предшествующего ON_PAUSE/ON_STOP не должен перезаписывать playWhenReady
        observer.onStateChanged(fakeLifecycleOwner, Lifecycle.Event.ON_RESUME)
        assertFalse(holder.playWhenReady)
    }
}
