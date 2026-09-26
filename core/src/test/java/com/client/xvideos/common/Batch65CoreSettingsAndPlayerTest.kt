package com.client.xvideos.common

import com.client.xvideos.common.applock.AppLockTimeout
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.settings.ThumbnailsSize
import com.client.xvideos.common.settings.calculateNextColumn
import com.client.xvideos.common.settings.calculatePrevColumn
import com.client.xvideos.common.settings.getEnabledColumns
import com.client.xvideos.common.storage.StorageCleanupGate
import com.client.xvideos.common.videoplayer.model.PlayerOption
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackConfig
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch65CoreSettingsAndPlayerTest {

    @Test
    fun `ThumbnailsSize cyclic navigation and validations`() {
        assertEquals(ThumbnailsSize.SMALL, ThumbnailsSize.XMAX.next())
        assertEquals(ThumbnailsSize.LARGE_THUMBALIST, ThumbnailsSize.SMALL.next())
        assertEquals(ThumbnailsSize.XMAX, ThumbnailsSize.LARGE_THUMBALIST.next())

        assertEquals(ThumbnailsSize.LARGE_THUMBALIST, ThumbnailsSize.XMAX.prev())
        assertEquals(ThumbnailsSize.XMAX, ThumbnailsSize.SMALL.prev())
        assertEquals(ThumbnailsSize.SMALL, ThumbnailsSize.LARGE_THUMBALIST.prev())

        assertEquals(ThumbnailsSize.XMAX, ThumbnailsSize.fromOrdinalOrDefault(0))
        assertEquals(ThumbnailsSize.SMALL, ThumbnailsSize.fromOrdinalOrDefault(1))
        assertEquals(ThumbnailsSize.DEFAULT, ThumbnailsSize.fromOrdinalOrDefault(999))

        assertTrue(ThumbnailsSize.isValidValue("xMax"))
        assertTrue(ThumbnailsSize.isValidValue("small"))
        assertFalse(ThumbnailsSize.isValidValue("invalid"))
        assertFalse(ThumbnailsSize.isValidValue(null))

        assertTrue(ThumbnailsSize.isValidDisplayName("Large"))
        assertFalse(ThumbnailsSize.isValidDisplayName("NonExistent"))

        assertTrue(ThumbnailsSize.XMAX.isLarge)
        assertTrue(ThumbnailsSize.SMALL.isMedium)
    }

    @Test
    fun `ColumnSelect calculation pure functions`() {
        val flags = listOf(false, true, true, false, true) // indices: 1, 2, 4 enabled

        assertEquals(listOf(1, 2, 4), getEnabledColumns(flags))
        assertEquals(emptyList<Int>(), getEnabledColumns(emptyList()))

        assertEquals(2, calculateNextColumn(1, flags))
        assertEquals(4, calculateNextColumn(2, flags))
        assertEquals(1, calculateNextColumn(4, flags))
        assertEquals(1, calculateNextColumn(3, flags)) // unknown index falls back to first

        assertEquals(4, calculatePrevColumn(1, flags))
        assertEquals(1, calculatePrevColumn(2, flags))
        assertEquals(2, calculatePrevColumn(4, flags))
        assertEquals(4, calculatePrevColumn(3, flags)) // unknown index falls back to last

        // Empty flags check
        assertEquals(2, calculateNextColumn(2, emptyList()))
        assertEquals(2, calculatePrevColumn(2, emptyList()))
    }

    @Test
    fun `ScrollButtonEffect helpers and navigation`() {
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.FLAT.next())
        assertEquals(ScrollButtonEffect.GLASS, ScrollButtonEffect.BLUR.next())
        assertEquals(ScrollButtonEffect.FLAT, ScrollButtonEffect.GLASS.next())

        assertEquals(ScrollButtonEffect.GLASS, ScrollButtonEffect.FLAT.prev())
        assertEquals(ScrollButtonEffect.FLAT, ScrollButtonEffect.BLUR.prev())
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.GLASS.prev())

        assertFalse(ScrollButtonEffect.FLAT.requiresBlurShader)
        assertTrue(ScrollButtonEffect.BLUR.requiresBlurShader)
        assertTrue(ScrollButtonEffect.GLASS.requiresBlurShader)

        assertEquals(ScrollButtonEffect.FLAT, ScrollButtonEffect.fromOrdinalOrDefault(0))
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.fromOrdinalOrDefault(99))

        assertEquals(ScrollButtonEffect.GLASS, ScrollButtonEffect.fromTitleOrDefault("Стекло"))
        assertEquals(ScrollButtonEffect.DEFAULT, ScrollButtonEffect.fromTitleOrDefault("Неизвестно"))
        assertTrue(ScrollButtonEffect.allTitles.contains("Заливка"))
    }

    @Test
    fun `AppLockTimeout navigation and queries`() {
        assertEquals(AppLockTimeout.SECONDS_30, AppLockTimeout.IMMEDIATELY.next())
        assertEquals(AppLockTimeout.NEVER, AppLockTimeout.MINUTES_5.next())
        assertEquals(AppLockTimeout.IMMEDIATELY, AppLockTimeout.NEVER.next())

        assertEquals(AppLockTimeout.NEVER, AppLockTimeout.IMMEDIATELY.prev())
        assertEquals(AppLockTimeout.MINUTES_5, AppLockTimeout.NEVER.prev())

        assertTrue(AppLockTimeout.IMMEDIATELY.isImmediate)
        assertTrue(AppLockTimeout.IMMEDIATELY.isImmediately)
        assertEquals(AppLockTimeout.MINUTES_1, AppLockTimeout.fromOrdinalOrDefault(2))
        assertEquals(AppLockTimeout.DEFAULT, AppLockTimeout.fromOrdinalOrDefault(99))
        assertTrue(AppLockTimeout.displayNames.contains("1 минута"))
    }

    @Test
    fun `PlayerSpeed and ScreenResize helpers`() {
        assertEquals(PlayerSpeed.X1_25, PlayerSpeed.X1.nextSpeed())
        assertEquals(PlayerSpeed.X0_75, PlayerSpeed.X1.prevSpeed())
        assertEquals(PlayerSpeed.X2, PlayerSpeed.X2.faster())
        assertEquals(PlayerSpeed.X1_25, PlayerSpeed.X1.faster())
        assertEquals(PlayerSpeed.X0_25, PlayerSpeed.X0_25.slower())
        assertEquals(PlayerSpeed.X0_75, PlayerSpeed.X1.slower())

        assertEquals(PlayerSpeed.X1, PlayerSpeed.closestSpeed(1.05f))
        assertEquals(PlayerSpeed.X2, PlayerSpeed.closestSpeed(3.0f))
        assertEquals(PlayerSpeed.X0_25, PlayerSpeed.closestSpeed(0.1f))

        assertEquals(PlayerSpeed.X1_5, PlayerSpeed.fromDisplayName("1.5x"))
        assertNull(PlayerSpeed.fromDisplayName(null))

        assertEquals(ScreenResize.FILL, ScreenResize.FIT.toggle())
        assertEquals(ScreenResize.FIT, ScreenResize.FILL.toggle())

        assertTrue(PlayerOption.SPEED.hasSubmenu)
        assertFalse(PlayerOption.NONE.hasSubmenu)
    }

    @Test
    fun `PlayerPlaybackConfig mutations and status`() {
        val config = PlayerPlaybackConfig(url = "https://example.com/video.mp4")

        val muted = config.withVolume(0f)
        assertTrue(muted.isMuted)
        assertFalse(muted.isAudioEnabled)

        val unmuted = muted.withVolume(0.8f)
        assertEquals(0.8f, unmuted.volume, 0.001f)
        assertTrue(unmuted.isAudioEnabled)

        val fast = config.withSpeed(PlayerSpeed.X1_5)
        assertEquals(PlayerSpeed.X1_5, fast.speed)

        val seeked = config.withSeek(15.5f)
        assertEquals(15.5f, seeked.seekToTime ?: 0f, 0.001f)
        assertTrue(seeked.isSeeking)

        val playedFrom = config.withPlayFromTime(3.0f)
        assertEquals(3.0f, playedFrom.playFromTime ?: 0f, 0.001f)

        val paused = config.toggledPlayPause()
        assertTrue(paused.isPause)
        assertFalse(paused.isPlaying)

        val unpaused = paused.toggledPlayPause()
        assertFalse(unpaused.isPause)
        assertTrue(unpaused.isPlaying)

        val looped = config.toggledLoop()
        assertTrue(looped.loop)

        val resized = config.toggledScreenResize()
        assertEquals(ScreenResize.FILL, resized.size)

        val outOfBounds = PlayerPlaybackConfig(
            url = "https://example.com/video.mp4",
            volume = 2.5f,
            seekToTime = -10f,
            playFromTime = -5f
        ).sanitized()

        assertEquals(1.0f, outOfBounds.volume, 0.001f)
        assertEquals(0f, outOfBounds.seekToTime ?: -1f, 0.001f)
        assertEquals(0f, outOfBounds.playFromTime ?: -1f, 0.001f)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `StorageCleanupGate status lifecycle and cancellation`() = runTest {
        val gate = StorageCleanupGate()
        gate.resetForTesting()

        assertEquals("Idle", gate.getStatusDescription())
        assertFalse(gate.isStarted)
        assertFalse(gate.hasJob)
        assertTrue(gate.isIdle)

        val testScope = TestScope(StandardTestDispatcher(testScheduler))
        gate.start(testScope) {
            // No-op
        }

        assertTrue(gate.hasJob)
        assertTrue(gate.isStarted)

        gate.cancel()
        assertFalse(gate.hasJob)
        assertEquals("Idle", gate.getStatusDescription())
    }
}
