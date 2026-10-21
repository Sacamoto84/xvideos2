package com.client.xvideos.common.videoplayer.model

import kotlin.math.abs

/**
 * Свайп по видео для перемотки на шаг: `+1` — вперёд (палец ушёл вправо),
 * `-1` — назад, `0` — не свайп. Свайп — смещение от касания до отпускания не
 * короче [minDistancePx] по горизонтали и вдвое больше вертикального: короткое
 * движение остаётся тапом, диагональ и вертикаль не перематывают.
 */
fun swipeSeekDirection(dx: Float, dy: Float, minDistancePx: Float): Int = when {
    abs(dx) < minDistancePx || abs(dx) <= 2 * abs(dy) -> 0
    dx > 0f -> 1
    else -> -1
}
