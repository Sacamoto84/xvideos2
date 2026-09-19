package com.client.xvideos.x.screens.videoplayer

import com.client.xvideos.x.screens.videoplayerFullScreen.ScreenX_VideoPlayerFullScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class ScreenX_VideoPlayerKeysTest {

    private inline fun <reified T : Serializable> assertJavaSerialization(screen: T): T {
        val byteOut = ByteArrayOutputStream()
        ObjectOutputStream(byteOut).use { it.writeObject(screen) }

        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        val restored = ObjectInputStream(byteIn).use { it.readObject() }

        assertNotNull(restored)
        return restored as T
    }

    @Test
    fun `ScreenX_VideoPlayer preserves deterministic key and survives serialization`() {
        val screen = ScreenX_VideoPlayer("https://example.com/video123")
        val restored = assertJavaSerialization(screen)
        assertEquals("https://example.com/video123", restored.url)
        assertEquals("ScreenX_VideoPlayer:https://example.com/video123", restored.key)
    }

    @Test
    fun `ScreenX_LocalVideoPlayer preserves deterministic key and survives serialization`() {
        val screen = ScreenX_LocalVideoPlayer("file:///data/local/vid_999.mp4")
        val restored = assertJavaSerialization(screen)
        assertEquals("file:///data/local/vid_999.mp4", restored.fileUrl)
        assertEquals("ScreenX_LocalVideoPlayer:file:///data/local/vid_999.mp4", restored.key)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `ScreenX_VideoPlayerFullScreen preserves deterministic key and survives serialization`() {
        val screen = ScreenX_VideoPlayerFullScreen("https://example.com/video_full")
        val restored = assertJavaSerialization(screen)
        assertEquals("https://example.com/video_full", restored.url)
        assertEquals("ScreenX_VideoPlayerFullScreen:https://example.com/video_full", restored.key)
    }
}
