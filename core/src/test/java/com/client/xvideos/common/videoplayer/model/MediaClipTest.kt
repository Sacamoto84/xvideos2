package com.client.xvideos.common.videoplayer.model

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Плеер играет отрезки одного файла по кругу (превью сохранённого ролика X):
 * для отрезков повторяется весь список, для одного ролика — он сам.
 */
class MediaClipTest {

    @Test
    fun `режим повтора — весь список для отрезков, один ролик без них`() {
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(isLooping = false, hasClips = false))
        assertEquals(Player.REPEAT_MODE_ONE, playerRepeatMode(isLooping = true, hasClips = false))
        assertEquals(Player.REPEAT_MODE_ALL, playerRepeatMode(isLooping = true, hasClips = true))
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(isLooping = false, hasClips = true))
    }
}
