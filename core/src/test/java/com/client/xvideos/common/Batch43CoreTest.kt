package com.client.xvideos.common

import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.net.UserAgentProvider
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.settings.ThumbnailsSize
import com.client.xvideos.common.storage.StorageCleanupGate
import com.client.xvideos.common.videoplayer.host.DrmConfig
import com.client.xvideos.common.videoplayer.model.PlayerOption
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackConfig
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch43CoreTest {

    @Test
    fun `ScrollButtonEffect helpers and lookup operate correctly`() {
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.DEFAULT)
        assertTrue(ScrollButtonEffect.FLAT.isFlat)
        assertFalse(ScrollButtonEffect.FLAT.isBlur)
        assertTrue(ScrollButtonEffect.BLUR.isBlur)
        assertTrue(ScrollButtonEffect.GLASS.isGlass)

        assertEquals(ScrollButtonEffect.FLAT, ScrollButtonEffect.fromNameOrDefault("flat"))
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.fromNameOrDefault("unknown"))
        assertEquals(ScrollButtonEffect.BLUR, ScrollButtonEffect.fromNameOrDefault(null))
    }

    @Test
    fun `ThumbnailsSize helpers and lookup operate correctly`() {
        assertEquals(ThumbnailsSize.SMALL, ThumbnailsSize.DEFAULT)
        assertTrue(ThumbnailsSize.XMAX.isXMax)
        assertTrue(ThumbnailsSize.SMALL.isSmall)
        assertTrue(ThumbnailsSize.LARGE_THUMBALIST.isLargeThumbnail)

        assertEquals(ThumbnailsSize.XMAX, ThumbnailsSize.fromValueOrDefault("xMax"))
        assertEquals(ThumbnailsSize.SMALL, ThumbnailsSize.fromValueOrDefault("unknown"))
        assertEquals(ThumbnailsSize.SMALL, ThumbnailsSize.fromValueOrDefault(null))
        assertEquals(ThumbnailsSize.SMALL, ThumbnailsSize.fromValueOrDefault("  "))
    }

    @Test
    fun `StorageCleanupGate helpers operate correctly`() {
        val gate = StorageCleanupGate()
        assertFalse(gate.isStarted)
        assertTrue(gate.isCompleted)
        assertFalse(gate.isActive)
        assertFalse(gate.isPending)

        gate.resetForTesting()
        assertFalse(gate.isStarted)
    }

    @Test
    fun `PlayerSpeed, ScreenResize, and PlayerOption operate correctly`() {
        assertTrue(PlayerSpeed.X0_5.isSlow)
        assertFalse(PlayerSpeed.X1.isSlow)
        assertFalse(PlayerSpeed.X1.isFast)
        assertTrue(PlayerSpeed.X1_5.isFast)

        assertEquals(ScreenResize.FIT, ScreenResize.DEFAULT)
        assertTrue(ScreenResize.FIT.isFit)
        assertTrue(ScreenResize.FILL.isFill)

        assertEquals(PlayerOption.NONE, PlayerOption.DEFAULT)
        assertTrue(PlayerOption.NONE.isNone)
        assertTrue(PlayerOption.SPEED.isSpeed)
        assertTrue(PlayerOption.QUALITY.isQuality)
        assertTrue(PlayerOption.AUDIO_TRACK.isAudioTrack)
        assertTrue(PlayerOption.SUBTITLES.isSubtitles)
    }

    @Test
    fun `PlayerPlaybackConfig helpers operate correctly`() {
        val empty = PlayerPlaybackConfig.EMPTY
        assertFalse(empty.isValid)
        assertTrue(empty.isEmpty)
        assertFalse(empty.isMuted)
        assertFalse(empty.hasHeaders)
        assertFalse(empty.hasDrm)
        assertFalse(empty.isSeeking)

        val mutedConfig = PlayerPlaybackConfig(
            url = "https://example/v.mp4",
            volume = 0f,
            headers = mapOf("Authorization" to "Bearer 123"),
            drmConfig = DrmConfig("kid1", "key1"),
            isSliding = true
        )
        assertTrue(mutedConfig.isValid)
        assertTrue(mutedConfig.isMuted)
        assertTrue(mutedConfig.hasHeaders)
        assertTrue(mutedConfig.hasDrm)
        assertTrue(mutedConfig.isSeeking)
    }

    @Test
    fun `UserAgentProvider operates correctly`() {
        assertTrue(UserAgentProvider.count > 0)
        assertEquals(UserAgentProvider.count, UserAgentProvider.allUserAgents.size)
        assertTrue(UserAgentProvider.defaultUserAgent.startsWith("Mozilla"))
        assertNotNull(UserAgentProvider.randomDesktopBrowser())
    }

    @Test
    fun `CollectionEntity and CollectionGridItem operate correctly`() {
        val entity = CollectionEntity(collection = "my_favs", items = listOf("1", "2"))
        assertTrue(entity.isValid)
        assertFalse(entity.isEmpty)
        assertTrue(entity.isNotEmpty)
        assertEquals(2, entity.size)

        val emptyEntity = CollectionEntity(collection = "", items = emptyList<String>())
        assertFalse(emptyEntity.isValid)
        assertTrue(emptyEntity.isEmpty)
        assertEquals(0, emptyEntity.size)

        val emptyItem = CollectionGridItem.EMPTY
        assertFalse(emptyItem.isValid)
        assertFalse(emptyItem.hasPreview)
        assertFalse(emptyItem.hasCount)

        val validItem = CollectionGridItem(name = "Col1", previewUrl = "https://cdn/p.jpg", itemsCount = 10)
        assertTrue(validItem.isValid)
        assertTrue(validItem.hasPreview)
        assertTrue(validItem.hasCount)
    }
}
