package com.client.xvideos.common.videoplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Преобразует численный масштаб в читаемый пользовательский статус.
 */
fun formatZoomLabel(scale: Float, fillScale: Float = 1.0f): String {
    if (!scale.isFinite() || scale <= 0f) return "100%"
    if (abs(scale - 1.0f) <= 0.02f) return "100%"
    if (fillScale.isFinite() && fillScale > 1.05f && abs(scale - fillScale) <= 0.04f) return "Во весь экран"
    val safeScale = scale.coerceIn(0.1f, 10f)
    return "${(safeScale * 100f).roundToInt()}%"
}

/**
 * Определяет, активно ли пользовательское увеличение кадра.
 */
fun isZoomActive(scale: Float): Boolean = scale.isFinite() && scale > 1.02f

/**
 * Всплывающий индикатор масштаба (HUD Pill Badge) поверх видеоплеера.
 *
 * Показывается по центру вверху экрана при изменении масштаба жестом щипка
 * или двойным тапом и автоматически скрывается спустя короткое время бездействия.
 */
@Composable
fun VideoZoomHud(
    scale: Float,
    modifier: Modifier = Modifier,
    fillScale: Float = 1.0f,
    onReset: (() -> Unit)? = null
) {
    var isVisible by remember { mutableStateOf(false) }
    var lastScale by remember { mutableFloatStateOf(scale) }

    LaunchedEffect(scale) {
        if (abs(scale - lastScale) > 0.01f) {
            lastScale = scale
            isVisible = true
            delay(1500)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val label = remember(scale, fillScale) { formatZoomLabel(scale, fillScale) }
        val active = isZoomActive(scale)

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .then(
                    if (active && onReset != null) {
                        Modifier.clickable { onReset() }
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (active && onReset != null) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Сбросить зум",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
