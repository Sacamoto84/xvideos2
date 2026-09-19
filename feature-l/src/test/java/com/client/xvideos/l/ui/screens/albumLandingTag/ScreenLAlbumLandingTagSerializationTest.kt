package com.client.xvideos.l.ui.screens.albumLandingTag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ScreenLAlbumLandingTagSerializationTest {

    @Test
    fun `ScreenLAlbumLandingTag survives Java object serialization in saved state`() {
        val screen = ScreenLAlbumLandingTag("cosplay")

        val byteOut = ByteArrayOutputStream()
        ObjectOutputStream(byteOut).use { it.writeObject(screen) }

        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        val restored = ObjectInputStream(byteIn).use { it.readObject() } as ScreenLAlbumLandingTag

        assertNotNull(restored)
        assertEquals("cosplay", restored.tag)
    }
}
