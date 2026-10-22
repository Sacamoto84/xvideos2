package com.client.xvideos.common.videoplayer.model

import androidx.compose.runtime.Immutable
import androidx.media3.common.Player

/**
 * Отрезок файла [startMs]…[endMs]. Плеер с отрезками играет их друг за
 * другом вместо файла целиком — так устроено превью сохранённого ролика X.
 */
@Immutable
data class MediaClip(val startMs: Long, val endMs: Long)

/**
 * Режим повтора плеера: без повтора — `OFF`; с отрезками повторяется весь
 * список (`ALL`), без них — сам ролик (`ONE`).
 */
fun playerRepeatMode(isLooping: Boolean, hasClips: Boolean): Int = when {
    !isLooping -> Player.REPEAT_MODE_OFF
    hasClips -> Player.REPEAT_MODE_ALL
    else -> Player.REPEAT_MODE_ONE
}
