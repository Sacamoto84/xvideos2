package com.client.xvideos.common.ui.edge

import androidx.compose.ui.geometry.Rect
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Сдвиг влево (px), чтобы блок `[0, width] x [0, height]` не лёг на вырез камеры.
 * [cutouts] — прямоугольники вырезов в координатах блока; вырез без значений
 * (NaN) или вне блока не учитывается. Блок встаёт левее выреза с зазором [gap].
 */
internal fun cutoutShiftLeft(cutouts: List<Rect>, width: Int, height: Int, gap: Int): Int =
    cutouts
        .filter { it.left < width && it.right > 0 && it.top < height && it.bottom > 0 }
        .maxOfOrNull { (width - it.left).roundToInt() + gap } ?: 0

/** Отражение по горизонтали внутри блока шириной [width]: левый край сводится к правому. */
internal fun Rect.mirroredIn(width: Int): Rect = Rect(width - right, top, width - left, bottom)

/**
 * Сдвиг влево (px), чтобы правый верхний угол блока шириной [width] не ушёл под
 * скругление правого верхнего угла экрана. Центр скругления ([centerX], [centerY])
 * и [radius] — в координатах блока; верх блока — y = 0.
 */
internal fun roundedCornerShiftLeft(width: Int, centerX: Float, centerY: Float, radius: Float, gap: Int): Int {
    // Дуга — только выше центра и правее него; NaN тоже отсекается здесь.
    if (!(width > centerX && centerY > 0f && radius > 0f)) return 0
    // Самая стеснённая точка блока — его верхняя кромка.
    val dy = centerY.coerceAtMost(radius)
    val visibleRight = centerX + sqrt(radius * radius - dy * dy)
    val overflow = width - visibleRight
    return if (overflow > 0f) ceil(overflow).toInt() + gap else 0
}
