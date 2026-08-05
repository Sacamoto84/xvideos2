package com.client.xvideos.r.common.video

import com.client.xvideos.common.theme.Theme

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.util.toMinSec
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun CanvasTimeDurationLine1(
    currentTime: Float,
    duration: Int,
    modifier: Modifier = Modifier,
    timeA: Float = 0f,
    timeB: Float = 1f,
    timeABEnable: Boolean,
    visibleAB: Boolean = true,
    play: Boolean = false,
    isBuffering: Boolean = false,
    onSeek: (Float) -> Unit,              // 🔹 при перетаскивании
    onSeekFinished: (() -> Unit)? = null, // 🔹 когда отпустили
    isVisibleTime: Boolean = true,
    isVisibleStep: Boolean = true
) {

    var isDragging by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "buffering")
    val bufferingColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFCDECFB),
        //targetValue = Color(0xFF137CBD), // Синий
        targetValue = Color(0xFF73CDF1),
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bufferingColor"
    )

    Row(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                coroutineScope {
                    while (true) {
                        awaitPointerEventScope {
                            val down = awaitFirstDown()

                            val downX = down.position.x
                            val newTime = (downX / size.width) * duration
                            onSeek(newTime.coerceIn(0f, duration.toFloat()))

                            var drag: PointerInputChange? = null
                            try {
                                drag = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                    // Начало drag
                                    isDragging = true
                                    change.consume()
                                }
                            } catch (_: CancellationException) {
                            }

                            if (drag != null) {
                                // Мы начали перетаскивать
                                horizontalDrag(drag.id) { change ->
                                    val dragX = change.position.x
                                    val newTimeDrag = (dragX / size.width) * duration
                                    onSeek(newTimeDrag.coerceIn(0f, duration.toFloat()))
                                    change.consume()
                                }
                                isDragging = false
                                onSeekFinished?.invoke()
                            } else {
                                // Просто тап, без драггинга
                                onSeekFinished?.invoke()
                            }
                        }
                    }
                }
            },

        verticalAlignment = Alignment.CenterVertically
    ) {

        if (isVisibleTime) {
            Box {
                Text(
                    " " + currentTime.toDouble().toMinSec(),
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    modifier = Modifier
                        .width(44.dp)
                        .offset(0.5.dp, 0.5.dp)//.background(Color.Green)
                )

                Text(
                    " " + currentTime.toDouble().toMinSec(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    modifier = Modifier.width(44.dp)//.background(Color.Green)
                )
            }
        }

        Canvas(modifier = Modifier
            .fillMaxSize()
            .weight(1f))
        {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val baseLineColor = if (isBuffering) bufferingColor else Color(0xff909090)
            val stepColor = if (isBuffering) bufferingColor else Color.Gray

            if ((duration > 0) && (isVisibleStep)) {
                val step = duration
                val stepW = canvasWidth / duration
                for (i in 0..step) {
                    drawLine(
                        color = stepColor,
                        start = Offset(x = i * stepW, y = canvasHeight / 2 - 0.dp.toPx()),
                        end = Offset(x = i * stepW, y = canvasHeight / 2 + 4.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Фон
            drawLine(
                color = baseLineColor,
                start = Offset(x = 0f, y = canvasHeight / 2),
                end = Offset(x = canvasWidth, y = canvasHeight / 2),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square
            )

            if (duration > 0) {

                val progressRatio = currentTime / duration

                val progressRatioA = canvasWidth * (timeA / duration).coerceIn(0f, 1f)
                val progressRatioB = canvasWidth * (timeB / duration).coerceIn(0f, 1f)
                val progressWidth = canvasWidth * progressRatio.coerceIn(0f, 1f)

                if (!timeABEnable) {
                    drawLine(
                        color = Color(0xFFE73538),
                        start = Offset(x = 0f, y = canvasHeight / 2),
                        end = Offset(x = progressWidth, y = canvasHeight / 2),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Square
                    )
                }

                // Индикатор (ползунок)
                drawCircle(
                    color = Color(0xFFE73538),
                    center = Offset(progressWidth, canvasHeight / 2),
                    radius = 3.dp.toPx()
                )

                // Отрезок A-B
                if (visibleAB) {
                    drawLine(
                        color = Color(0xFF8BC34A),
                        start = Offset(x = progressRatioA, y = canvasHeight / 2 - 4.dp.toPx()),
                        end = Offset(x = progressRatioB, y = canvasHeight / 2 - 4.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Square
                    )
                }

            }
        }



        if (isVisibleTime) {

            Box {

                Text(
                    duration.toDouble().toMinSec() + " ",
                    color = Color.Black,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    modifier = Modifier
                        .width(44.dp)
                        .offset(0.5.dp, 0.5.dp)//.background(Color.Green)
                )

                Text(
                    duration.toDouble().toMinSec() + " ",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    fontFamily = Theme.R.fontFamilyPopinsRegular,
                    modifier = Modifier.width(44.dp)//.background(Color.Green)
                )
            }

        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, device = "id:Nexus 5", apiLevel = 34)
@Composable
fun CanvasTimeDurationLine1Preview() {

    CanvasTimeDurationLine1(
        currentTime = 7f,
        duration = 20,
        timeA = 2f,
        timeB = 5f,
        timeABEnable = false,
        visibleAB = true,
        play = false,
        onSeek = { },
        onSeekFinished = { },
        isVisibleTime = true,
        isVisibleStep = true
    )

}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun CanvasTimeDurationLine1BufferingPreview() {
    CanvasTimeDurationLine1(
        currentTime = 5f,
        duration = 20,
        timeABEnable = false,
        isBuffering = true,
        onSeek = { },
        onSeekFinished = { }
    )
}
