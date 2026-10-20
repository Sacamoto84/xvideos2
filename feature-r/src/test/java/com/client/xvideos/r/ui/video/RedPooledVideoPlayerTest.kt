package com.client.xvideos.r.ui.video

import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class RedPooledVideoPlayerTest {

    @Test
    fun `clampSeekPositionMs clamps negative position to zero when duration is unset`() {
        assertEquals(0L, clampSeekPositionMs(-500L, C.TIME_UNSET))
    }

    @Test
    fun `clampSeekPositionMs preserves positive position when duration is unset`() {
        assertEquals(1500L, clampSeekPositionMs(1500L, C.TIME_UNSET))
    }

    @Test
    fun `clampSeekPositionMs clamps negative position to zero when duration is known`() {
        assertEquals(0L, clampSeekPositionMs(-100L, 5000L))
    }

    @Test
    fun `clampSeekPositionMs clamps position exceeding duration to duration`() {
        assertEquals(5000L, clampSeekPositionMs(6000L, 5000L))
    }

    @Test
    fun `clampSeekPositionMs preserves position within bounds`() {
        assertEquals(2500L, clampSeekPositionMs(2500L, 5000L))
    }

    @Test
    fun `clampSeekPositionMs handles exact boundaries correctly`() {
        assertEquals(0L, clampSeekPositionMs(0L, 5000L))
        assertEquals(5000L, clampSeekPositionMs(5000L, 5000L))
    }

    @Test
    fun `calculateDragDeltaMs returns zero for zero or non-finite drag`() {
        assertEquals(0L, calculateDragDeltaMs(0f))
        assertEquals(0L, calculateDragDeltaMs(Float.NaN))
        assertEquals(0L, calculateDragDeltaMs(Float.POSITIVE_INFINITY))
    }

    @Test
    fun `calculateDragDeltaMs returns one frame for small positive and negative drag`() {
        val frameMs = (1000f / 30f).toLong()
        assertEquals(frameMs, calculateDragDeltaMs(50f))
        assertEquals(-frameMs, calculateDragDeltaMs(-50f))
    }

    @Test
    fun `calculateDragDeltaMs returns one second for large drag exceeding threshold`() {
        assertEquals(1000L, calculateDragDeltaMs(450f))
        assertEquals(-1000L, calculateDragDeltaMs(-450f))
    }

    @Test
    fun `isValidABRange rejects NaN, infinite points, negative timeA, and inverted range`() {
        assertFalse(isValidABRange(enableAB = false, 1f, 5f))
        assertFalse(isValidABRange(enableAB = true, Float.NaN, 5f))
        assertFalse(isValidABRange(enableAB = true, 1f, Float.NaN))
        assertFalse(isValidABRange(enableAB = true, Float.NEGATIVE_INFINITY, 5f))
        assertFalse(isValidABRange(enableAB = true, 1f, Float.POSITIVE_INFINITY))
        assertFalse(isValidABRange(enableAB = true, -1f, 5f))
        assertFalse(isValidABRange(enableAB = true, -0.001f, 5f))
        assertFalse(isValidABRange(enableAB = true, 5f, 1f))
        assertFalse(isValidABRange(enableAB = true, 3f, 3f))
        assertTrue(isValidABRange(enableAB = true, 0f, 5f))
        assertTrue(isValidABRange(enableAB = true, 1f, 5f))
    }

    @Test
    fun `isZoomActive correctly detects active zoom state for pager and seek lock`() {
        assertFalse(com.client.xvideos.common.videoplayer.ui.isZoomActive(1.0f))
        assertFalse(com.client.xvideos.common.videoplayer.ui.isZoomActive(1.01f))
        assertTrue(com.client.xvideos.common.videoplayer.ui.isZoomActive(1.03f))
        assertTrue(com.client.xvideos.common.videoplayer.ui.isZoomActive(2.0f))
    }

    /** Плеер, у которого есть только playWhenReady и слушатели. */
    private class FakePlayer(var playWhenReady: Boolean) {
        val listeners = mutableListOf<Player.Listener>()

        val player: Player = Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java),
            InvocationHandler { _, method, args ->
                when (method.name) {
                    "getPlayWhenReady" -> playWhenReady
                    "addListener" -> listeners.add(args[0] as Player.Listener).let { null }
                    "removeListener" -> listeners.remove(args[0] as Player.Listener).let { null }
                    "toString" -> "FakePlayer"
                    "hashCode" -> 1
                    "equals" -> false
                    else -> when (method.returnType) {
                        Boolean::class.javaPrimitiveType -> false
                        Int::class.javaPrimitiveType -> 0
                        Long::class.javaPrimitiveType -> 0L
                        Float::class.javaPrimitiveType -> 0f
                        else -> null
                    }
                }
            }
        ) as Player

        fun changePlayWhenReady(value: Boolean) {
            playWhenReady = value
            listeners.toList().forEach {
                it.onPlayWhenReadyChanged(value, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            }
        }
    }

    @Test
    fun `awaitPlayWhenReady returns at once when the player already plays`() = runTest {
        val fake = FakePlayer(playWhenReady = true)

        fake.player.awaitPlayWhenReady()

        assertTrue(fake.listeners.isEmpty())
    }

    @Test
    fun `awaitPlayWhenReady waits until playWhenReady is set again and removes its listener`() = runTest {
        val fake = FakePlayer(playWhenReady = false)

        // Ушли в фон: LifecycleStartEffect снял playWhenReady (проход 203, C1)
        val wait = launch { fake.player.awaitPlayWhenReady() }
        runCurrent()
        assertTrue(wait.isActive)
        assertEquals(1, fake.listeners.size)

        fake.changePlayWhenReady(false)
        runCurrent()
        assertTrue(wait.isActive)

        // Вернулись: ON_START снова поставил playWhenReady
        fake.changePlayWhenReady(true)
        runCurrent()
        assertTrue(wait.isCompleted)
        assertTrue(fake.listeners.isEmpty())
    }

    @Test
    fun `cancelled awaitPlayWhenReady removes its listener`() = runTest {
        val fake = FakePlayer(playWhenReady = false)

        val wait = launch { fake.player.awaitPlayWhenReady() }
        runCurrent()
        wait.cancel()
        runCurrent()

        assertTrue(wait.isCancelled)
        assertTrue(fake.listeners.isEmpty())
    }
}
