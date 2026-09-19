package com.client.xvideos.x.screens.videoplayer

import com.client.xvideos.x.feature.country.CountryState
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.videoplayerFullScreen.ScreenX_VideoPlayerFullScreen
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ScreenX_VideoPlayerSerializationTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos -> oos.writeObject(value) }
            baos.toByteArray()
        }
        return ByteArrayInputStream(bytes).use { bais ->
            ObjectInputStream(bais).use { ois -> ois.readObject() as T }
        }
    }

    @Test
    fun `ScreenX_VideoPlayer serializes and deserializes properly with null item`() {
        val screen = ScreenX_VideoPlayer(url = "https://example.com/video1", item = null)
        val restored = roundTrip(screen)
        assertEquals("https://example.com/video1", restored.url)
        assertEquals(null, restored.item)
    }

    @Test
    fun `ScreenX_VideoPlayer serializes and deserializes properly with populated item`() {
        val item = ItemsX(id = 999L, title = "Sample Video", href = "/video999/")
        val screen = ScreenX_VideoPlayer(url = "https://example.com/video999", item = item)
        val restored = roundTrip(screen)
        assertEquals("https://example.com/video999", restored.url)
        assertEquals(999L, restored.item?.id)
        assertEquals("Sample Video", restored.item?.title)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `ScreenX_VideoPlayerFullScreen serializes and deserializes with position`() {
        val screen = ScreenX_VideoPlayerFullScreen(url = "https://example.com/hls.m3u8", position = 42000L)
        val restored = roundTrip(screen)
        assertEquals("https://example.com/hls.m3u8", restored.url)
        assertEquals(42000L, restored.position)
    }

    @Test
    fun `CountryState updates current and tracks user selection epoch`() {
        val initialEpoch = CountryState.userSelectionEpoch
        CountryState.updateCurrent("🇺🇸")
        assertEquals("🇺🇸", CountryState.current)
        assertEquals(initialEpoch, CountryState.userSelectionEpoch)

        CountryState.onCountrySelected("🇯🇵")
        assertEquals("🇯🇵", CountryState.current)
        assertEquals(initialEpoch + 1, CountryState.userSelectionEpoch)
    }
}
