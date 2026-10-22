package com.client.xvideos.common.videoplayer.model

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // Превью из отрезков без звука, но плеер всё равно декодировал звук и
    // создавал новый AudioTrack на каждой смене отрезка (телефон: 32 за ~20 с
    // на два превью).
    @Test
    fun `у превью из отрезков звуковая дорожка выключена, у ролика — нет`() {
        val base = TrackSelectionParameters.DEFAULT

        assertTrue(C.TRACK_TYPE_AUDIO in base.withAudioDisabledForClips(hasClips = true).disabledTrackTypes)
        assertFalse(C.TRACK_TYPE_AUDIO in base.withAudioDisabledForClips(hasClips = false).disabledTrackTypes)
    }

    @Test
    fun `режим повтора — весь список для отрезков, один ролик без них`() {
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(isLooping = false, hasClips = false))
        assertEquals(Player.REPEAT_MODE_ONE, playerRepeatMode(isLooping = true, hasClips = false))
        assertEquals(Player.REPEAT_MODE_ALL, playerRepeatMode(isLooping = true, hasClips = true))
        assertEquals(Player.REPEAT_MODE_OFF, playerRepeatMode(isLooping = false, hasClips = true))
    }
}
