package com.client.xvideos.l.ui.screens.screenFullScreen

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Шахматная подложка — под прозрачными картинками видно, где прозрачность.
 *
 * Выделено из `L_FullScreenImage.kt` (было 800 строк). Тело не менялось —
 * перенос дословный.
 */
fun Modifier.checkerboardBackground(
    squareSize: Dp = 8.dp,
    lightColor: Color = Color.White,
    darkColor: Color = Color.LightGray
): Modifier = this.then(
    Modifier.drawBehind {
        val squareSizePx = squareSize.toPx()
        val squaresHorizontal = (size.width / squareSizePx).toInt() + 1
        val squaresVertical = (size.height / squareSizePx).toInt() + 1

        for (i in 0..squaresHorizontal) {
            for (j in 0..squaresVertical) {
                val isLightSquare = (i + j) % 2 == 0
                val color = if (isLightSquare) lightColor else darkColor

                drawRect(
                    color = color,
                    topLeft = Offset(
                        x = i * squareSizePx,
                        y = j * squareSizePx
                    ),
                    size = Size(squareSizePx, squareSizePx)
                )
            }
        }
    }
)
