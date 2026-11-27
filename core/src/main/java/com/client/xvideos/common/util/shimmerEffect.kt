package com.client.xvideos.common.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val SHIMMER_COLORS = listOf(
    Color(0xFFB8B5B5),
    Color(0xFF8F8B8B),
    Color(0xFFB8B5B5),
)

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    return drawBehind {
        val width = size.width
        if (width <= 0f) return@drawBehind
        val startX = progress * width
        drawRect(
            brush = Brush.linearGradient(
                colors = SHIMMER_COLORS,
                start = Offset(startX, 0f),
                end = Offset(startX + width, 0f)
            )
        )
    }
}
