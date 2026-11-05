package com.client.xvideos.common.ui.atom

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.ui.composition.LocalScrollbarAlpha
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

internal const val SCROLLBAR_FULL_HEIGHT_TOLERANCE = 0.001f

internal val DefaultScrollbarSizeAnimationSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

internal fun shouldDrawScrollbar(
    startFraction: Float,
    endFraction: Float,
    tolerance: Float = SCROLLBAR_FULL_HEIGHT_TOLERANCE
): Boolean {
    if (endFraction <= startFraction) return false
    val span = endFraction - startFraction
    return span < (1f - tolerance)
}

/**
 * Вычисляет вертикальное смещение и высоту полосы прокрутки с защитой
 * от выхода за границы дорожки.
 */
internal fun calculateScrollbarOffsetYAndHeight(
    trackHeight: Float,
    startFraction: Float,
    span: Float
): Pair<Float, Float> {
    if (trackHeight <= 0f || span <= 0f) return 0f to 0f
    val clampedStart = startFraction.coerceIn(0f, 1f)
    val clampedSpan = span.coerceIn(0f, 1f)
    val rawOffsetY = trackHeight * clampedStart
    val rawHeight = trackHeight * clampedSpan
    val maxOffsetY = (trackHeight - rawHeight).coerceAtLeast(0f)
    val indicatorOffsetY = rawOffsetY.coerceIn(0f, maxOffsetY)
    val indicatorHeight = rawHeight.coerceIn(0f, trackHeight - indicatorOffsetY)
    return indicatorOffsetY to indicatorHeight
}

/**
 * Индикатор прокрутки: закрашивает ту долю дорожки, которая сейчас видна.
 *
 * При изменении размера (например, по мере подгрузки страниц альбома) размер
 * индикатора плавно анимируется, избавляя от резких скачков.
 */
@Composable
fun VerticalScrollbar(scrollPercent: () -> Pair<Float, Float>) {
    val alphaProvider = LocalScrollbarAlpha.current
    val spanAnim = remember { Animatable(Float.NaN) }

    LaunchedEffect(Unit) {
        snapshotFlow {
            val range = scrollPercent()
            val start = range.first.coerceIn(0f, 1f)
            val end = range.second.coerceIn(0f, 1f)
            if (shouldDrawScrollbar(start, end)) {
                (end - start).coerceIn(0f, 1f)
            } else {
                Float.NaN
            }
        }.collectLatest { targetSpan ->
            if (targetSpan.isNaN()) {
                spanAnim.snapTo(Float.NaN)
            } else if (spanAnim.value.isNaN()) {
                spanAnim.snapTo(targetSpan)
            } else if (abs(spanAnim.targetValue - targetSpan) > SCROLLBAR_FULL_HEIGHT_TOLERANCE) {
                spanAnim.animateTo(
                    targetValue = targetSpan,
                    animationSpec = DefaultScrollbarSizeAnimationSpec
                )
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val alpha = alphaProvider()
        if (alpha <= 0f) return@Canvas

        val range = scrollPercent()
        val trackHeight = size.height
        if (range.second <= range.first || trackHeight <= 0f) return@Canvas

        val startFraction = range.first.coerceIn(0f, 1f)
        val endFraction = range.second.coerceIn(0f, 1f)
        if (!shouldDrawScrollbar(startFraction, endFraction)) return@Canvas

        val targetSpan = (endFraction - startFraction).coerceIn(0f, 1f)
        val animValue = spanAnim.value
        val span = if (!animValue.isNaN()) animValue.coerceIn(0f, 1f) else targetSpan

        val (indicatorOffsetY, indicatorHeight) = calculateScrollbarOffsetYAndHeight(
            trackHeight = trackHeight,
            startFraction = startFraction,
            span = span
        )

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
