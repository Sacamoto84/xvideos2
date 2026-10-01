package com.client.xvideos.common.ui.atom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/**
 * Индикатор прокрутки: закрашивает ту долю дорожки, которая сейчас видна.
 */
@Composable
fun VerticalScrollbar(scrollPercent: () -> Pair<Float, Float>) {
    val alphaProvider = LocalScrollbarAlpha.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val alpha = alphaProvider()
        if (alpha <= 0f) return@Canvas

        val range = scrollPercent()
        val trackHeight = size.height
        if (range.second <= range.first || trackHeight <= 0f) return@Canvas

        val startFraction = range.first.coerceIn(0f, 1f)
        val endFraction = range.second.coerceIn(0f, 1f)

        val indicatorOffsetY = trackHeight * startFraction
        val indicatorHeight = (trackHeight * (endFraction - startFraction))
            .coerceAtLeast(0f)
            // Предохранитель, чтобы индикатор не вылез за нижний край дорожки.
            .coerceAtMost(trackHeight - indicatorOffsetY)

        if (indicatorHeight > 0f) {
            drawRect(
                color = if (alpha >= 1f) Color.Gray else Color.Gray.copy(alpha = Color.Gray.alpha * alpha),
                topLeft = Offset(x = 0f, y = indicatorOffsetY),
                size = Size(width = size.width, height = indicatorHeight)
            )
        }
    }
}

@Preview
@Composable
private fun VerticalScrollbarPreview() {
    VerticalScrollbar { 0.2f to 0.6f }
}
