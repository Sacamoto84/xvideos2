package com.client.xvideos.x.screens.tags

import com.client.xvideos.x.screens.dashboards.ScreenXDashBoards
import com.client.xvideos.x.screens.favorites.ScreenFavorites
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ScreenTagsSerializationTest {

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
    fun `ScreenTags serializes and deserializes properly with stable key`() {
        val screen = ScreenTags("cosplay")
        val restored = roundTrip(screen)
        assertEquals("cosplay", restored.tag)
        assertEquals("ScreenTags:cosplay", restored.key)
    }

    @Test
    fun `ScreenFavorites serializes and deserializes with stable key`() {
        val screen = ScreenFavorites()
        val restored = roundTrip(screen)
        assertEquals("ScreenFavorites", restored.key)
    }

    @Test
    fun `ScreenXDashBoards serializes and deserializes with stable key`() {
        val screen = ScreenXDashBoards()
        val restored = roundTrip(screen)
        assertEquals("ScreenXDashBoards", restored.key)
    }
}
