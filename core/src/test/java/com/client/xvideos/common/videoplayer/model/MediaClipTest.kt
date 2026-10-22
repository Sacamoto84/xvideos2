package com.client.xvideos.common.videoplayer.model

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Плеер играет отрезки одного файла по кругу (превью сохранённого ролика X):
 * для отрезков повторяется весь список, для одного ролика — он сам.
 */
class MediaClipTest {

    // С буфером полноэкранного плеера (до 45 с) плейлист секундных отрезков
    // держал впереди десятки периодов, и каждый заново разбирал весь файл —
    // десятки мегабайт на одно превью (финальное ревью плана, I1).
    @Test
    fun `буфер превью из отрезков — на пару отрезков вперёд, у ролика — прежний`() {
        val clips = playerBufferWindow(hasClips = true)
        assertTrue("впереди не больше трёх секундных отрезков", clips.maxMs <= 3_000)
        assertTrue(clips.minMs <= clips.maxMs)

        assertEquals(BufferWindow(minMs = 12_000, maxMs = 45_000), playerBufferWindow(hasClips = false))
    }

    @Test
    fun `режим повтора — весь список для отрезков, один ролик без них`() {
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(isLooping = false, hasClips = false))
        assertEquals(Player.REPEAT_MODE_ONE, playerRepeatMode(isLooping = true, hasClips = false))
        assertEquals(Player.REPEAT_MODE_ALL, playerRepeatMode(isLooping = true, hasClips = true))
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(isLooping = false, hasClips = true))
    }
}
