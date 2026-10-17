package com.client.xvideos.l.ui.screens.screenFullScreen.model

import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

/**
 * Сдвиг влево (px), чтобы блок `[0, width] x [0, height]` не лёг на вырез камеры.
 * [cutouts] — прямоугольники вырезов в координатах блока; вырез без значений
 * (NaN) или вне блока не учитывается. Блок встаёт левее выреза с зазором [gap].
 */
internal fun cutoutShiftLeft(cutouts: List<Rect>, width: Int, height: Int, gap: Int): Int =
    cutouts
        .filter { it.left < width && it.right > 0 && it.top < height && it.bottom > 0 }
        .maxOfOrNull { (width - it.left).roundToInt() + gap } ?: 0
