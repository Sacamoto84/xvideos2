package com.client.xvideos.r.ui.explorer.tab.saved.tab.collection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class SavedCollectionNameSerializationTest {

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
    fun `ScreenCollectionName serializes and deserializes properly`() {
        val screen = ScreenCollectionName(collectionName = "Favorites2026", popOnBack = true)
        val restored = roundTrip(screen)
        assertEquals("Favorites2026", restored.collectionName)
        assertEquals("RCollection:Favorites2026:true", restored.key)
    }
}
