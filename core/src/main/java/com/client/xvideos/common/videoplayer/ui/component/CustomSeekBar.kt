package com.client.xvideos.common.videoplayer.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Preview
@Composable
fun CustomSeekBarPreview() {
    CustomSeekBar(
        progress = 0.5f,
        maxProgress = 1f,
        onValueChange = {},
        onValueChangeFinished = {},
        trackHeight = 32.dp,
        thumbRadius = 32.dp,
        showThumbAlways = true,
    )
}




@Composable
fun CustomSeekBar(
    modifier: Modifier = Modifier,
    progress: Float,
    maxProgress: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    thumbRadius: Dp,
    trackHeight: Dp,
    activeTrackColor: Color = Color.Red,
    inactiveTrackColor: Color = Color.Gray,
    thumbColor: Color = Color.White,
    rippleColor: Color = Color.White.copy(alpha = 0.3f), // Soft glow effect
    showThumbAlways: Boolean = false,
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var localProgress by remember { mutableFloatStateOf(progress) }
    var trackWidth by remember { mutableFloatStateOf(1f) }
    var dragStartOffsetX by remember { mutableFloatStateOf(0f) }
    var initialProgress by remember { mutableFloatStateOf(0f) }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    // Animate ripple radius when dragging
    val rippleRadius by animateFloatAsState(
        targetValue = if (isDragging) with(density) { (thumbRadius * 2f).toPx() } else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "Ripple Animation"
    )

    LaunchedEffect(progress, maxProgress) {
        if (!isDragging) {
            localProgress = progress
        }
    }

    Box(
        modifier = modifier
            .height(thumbRadius * 2)
            .onSizeChanged { newSize -> trackWidth = newSize.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(maxProgress) {
                detectTapGestures { offset ->
                    val newValue = calculateCustomSeekBarSeekPosition(offset.x, trackWidth, maxProgress)
                    localProgress = newValue
                    currentOnValueChange(localProgress)
                    currentOnValueChangeFinished()
                }
            }
            .pointerInput(maxProgress) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragStartOffsetX = offset.x
                        initialProgress = calculateCustomSeekBarSeekPosition(dragStartOffsetX, trackWidth, maxProgress)
                        localProgress = initialProgress
                        currentOnValueChange(localProgress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localProgress = calculateCustomSeekBarDragDelta(dragAmount.x, trackWidth, maxProgress, localProgress)
                        currentOnValueChange(localProgress)
                    },
                    onDragEnd = {
                        isDragging = false
                        currentOnValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        currentOnValueChangeFinished()
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.CenterStart)
        ) {
            val trackWidthPx = size.width
            val thumbPx = with(density) { thumbRadius.toPx() }
            val progressX = calculateCustomSeekBarProgressX(localProgress, maxProgress, trackWidthPx)

            // Inactive track
            drawLine(
                color = inactiveTrackColor,
                start = Offset(0f, size.height / 2),
                end = Offset(trackWidthPx, size.height / 2),
                strokeWidth = with(density) { trackHeight.toPx() }
            )

            // Active track
            drawLine(
                color = activeTrackColor,
                start = Offset(0f, size.height / 2),
                end = Offset(progressX, size.height / 2),
                strokeWidth = with(density) { trackHeight.toPx() }
            )

            // Ripple Effect (Only when dragging)
            if (isDragging && rippleRadius > 0f) {
                drawCircle(
                    color = rippleColor,
                    radius = rippleRadius,
                    center = Offset(progressX, size.height / 2)
                )
            }

            // Thumb
            if (isDragging || showThumbAlways) {
                drawCircle(
                    color = thumbColor,
                    radius = thumbPx,
                    center = Offset(progressX, size.height / 2)
                )
            }
        }
    }
}

internal fun calculateCustomSeekBarProgressX(
    localProgress: Float,
    maxProgress: Float,
    trackWidthPx: Float
): Float {
    if (trackWidthPx <= 0f || maxProgress <= 0f) return 0f
    if (!localProgress.isFinite() || !maxProgress.isFinite() || !trackWidthPx.isFinite()) return 0f
    val ratio = (localProgress / maxProgress).coerceIn(0f, 1f)
    return (ratio * trackWidthPx).coerceIn(0f, trackWidthPx)
}

internal fun calculateCustomSeekBarSeekPosition(
    offsetX: Float,
    trackWidthPx: Float,
    maxProgress: Float
): Float {
    if (trackWidthPx <= 0f || maxProgress <= 0f) return 0f
    if (!offsetX.isFinite() || !maxProgress.isFinite() || !trackWidthPx.isFinite()) return 0f
    val ratio = (offsetX / trackWidthPx).coerceIn(0f, 1f)
    return (ratio * maxProgress).coerceIn(0f, maxProgress)
}

internal fun calculateCustomSeekBarDragDelta(
    dragAmountX: Float,
    trackWidthPx: Float,
    maxProgress: Float,
    currentProgress: Float
): Float {
    if (!currentProgress.isFinite()) return 0f
    if (trackWidthPx <= 0f || maxProgress <= 0f || !dragAmountX.isFinite()) {
        return currentProgress.coerceIn(0f, maxProgress.coerceAtLeast(0f))
    }
    val delta = (dragAmountX / trackWidthPx) * maxProgress
    return (currentProgress + delta).coerceIn(0f, maxProgress)
}


