package com.client.xvideos.common.videoplayer.model

import androidx.compose.runtime.Immutable
import androidx.media3.common.Player

/**
 * Отрезок файла [startMs]…[endMs]. Плеер с отрезками играет их друг за
 * другом вместо файла целиком — так устроено превью сохранённого ролика X.
 */
@Immutable
data class MediaClip(val startMs: Long, val endMs: Long)

/** Сколько плеер держит в буфере: не меньше [minMs], не больше [maxMs]. */
@Immutable
data class BufferWindow(val minMs: Int, val maxMs: Int)

/**
 * Буфер плеера. У превью из отрезков — на пару секундных отрезков вперёд:
 * с буфером ролика (до 45 с) плейлист по кругу держал впереди десятки
 * отрезков, и каждый заново разбирал таблицы всего файла.
 */
fun playerBufferWindow(hasClips: Boolean): BufferWindow =
    if (hasClips) BufferWindow(minMs = 2_000, maxMs = 3_000) else BufferWindow(minMs = 12_000, maxMs = 45_000)

/**
 * Режим повтора плеера: без повтора — `OFF`; с отрезками повторяется весь
 * список (`ALL`), без них — сам ролик (`ONE`).
 */
fun playerRepeatMode(isLooping: Boolean, hasClips: Boolean): Int = when {
    !isLooping -> Player.REPEAT_MODE_OFF
    hasClips -> Player.REPEAT_MODE_ALL
    else -> Player.REPEAT_MODE_ONE
}
