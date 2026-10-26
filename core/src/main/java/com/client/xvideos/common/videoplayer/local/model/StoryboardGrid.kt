package com.client.xvideos.common.videoplayer.local.model

import androidx.compose.ui.unit.IntRect
import com.client.xvideos.common.videoplayer.storyboard.STORYBOARD_COLUMNS
import com.client.xvideos.common.videoplayer.storyboard.STORYBOARD_ROWS
import kotlin.math.floor

/**
 * Раскадровка ролика X (`thumbSlideBig`) — одна картинка с сеткой 10×10 кадров,
 * по строкам слева направо; кадр `i` — позиция `i`% длины ролика. Так же номер
 * кадра считает плеер сайта.
 */
private const val STORYBOARD_FRAMES = STORYBOARD_COLUMNS * STORYBOARD_ROWS
private const val PERCENT = 100f

/**
 * Номер кадра для позиции [positionSec] ролика длиной [durationSec], 0…99.
 * Без длительности или при невалидной позиции — 0.
 *
 * `positionSec * 100 / durationSec`, а не `positionSec / durationSec * 100`: во
 * втором порядке ровно на границе процента получается `0.99999994` вместо `1`.
 */
fun storyboardFrameIndex(positionSec: Float, durationSec: Int): Int {
    if (durationSec <= 0 || !positionSec.isFinite()) return 0
    val index = floor(positionSec * PERCENT / durationSec).toInt()
    return index.coerceIn(0, STORYBOARD_FRAMES - 1)
}

/**
 * Прямоугольник кадра [index] в картинке раскадровки [imageWidth]×[imageHeight].
 * Размер кадра берётся из картинки, а не зашит: постоянна сетка, а не размер.
 * Картинка меньше сетки (кадр в 0 px) — `null`.
 */
fun storyboardFrameRect(index: Int, imageWidth: Int, imageHeight: Int): IntRect? {
    val frameWidth = imageWidth / STORYBOARD_COLUMNS
    val frameHeight = imageHeight / STORYBOARD_ROWS
    if (frameWidth <= 0 || frameHeight <= 0) return null
    val safeIndex = index.coerceIn(0, STORYBOARD_FRAMES - 1)
    val left = (safeIndex % STORYBOARD_COLUMNS) * frameWidth
    val top = (safeIndex / STORYBOARD_COLUMNS) * frameHeight
    return IntRect(left, top, left + frameWidth, top + frameHeight)
}

/**
 * Горизонтальное положение бегунка в координатах оверлея. Доля та же, что у
 * `calculateCustomSeekBarProgressX` в `CustomSeekBar`: полоса рисуется на всю
 * ширину бара, бегунок — в `доля × ширина`.
 */
fun seekPreviewAnchorX(
    positionSec: Float,
    durationSec: Int,
    trackStartPx: Float,
    trackWidthPx: Float,
    overlayStartPx: Float,
): Float {
    val ratio = if (durationSec > 0 && positionSec.isFinite()) {
        (positionSec / durationSec).coerceIn(0f, 1f)
    } else {
        0f
    }
    return trackStartPx - overlayStartPx + ratio * trackWidthPx
}

/**
 * Левый край кадра A: центр над бегунком, но у краёв кадр упирается и с
 * отступом [edgeMargin] дальше не едет. Не влезает даже с отступами — по центру.
 */
fun seekPreviewFollowLeft(anchorX: Float, frameWidth: Float, containerWidth: Float, edgeMargin: Float): Float {
    val maxLeft = containerWidth - frameWidth - edgeMargin
    if (maxLeft < edgeMargin) return (containerWidth - frameWidth) / 2f
    return (anchorX - frameWidth / 2f).coerceIn(edgeMargin, maxLeft)
}
