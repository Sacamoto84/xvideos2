package com.client.xvideos.l.ui.screens.screenAlbum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ScreenLAlbumSerializationTest {

    @Test
    fun `ScreenLAlbum survives Java object serialization in saved state and maintains deterministic key`() {
        val screen = ScreenLAlbum(12345L)

        val byteOut = ByteArrayOutputStream()
        ObjectOutputStream(byteOut).use { it.writeObject(screen) }

        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        val restored = ObjectInputStream(byteIn).use { it.readObject() } as ScreenLAlbum

        assertNotNull(restored)
        assertEquals(12345L, restored.idAlbum)
        assertEquals("LAlbum:12345", restored.key)
    }
}
