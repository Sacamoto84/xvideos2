package com.client.xvideos.common.ui.atom

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * CompositionLocal для динамического управления прозрачностью / видимостью скроллбара.
 * Передаётся как lambda `() -> Float` (от 0f до 1f), чтобы чтение происходило
 * исключительно в фазе отрисовки (внутри Canvas) без рекомпозиции всего экрана.
 */
val LocalScrollbarAlpha = compositionLocalOf<() -> Float> { { 1f } }

/**
 * Оборачивает содержимое пейджера и скрывает скроллбары во время движения пейджера.
 * При начале свайпа скроллбар моментально исчезает (snapTo 0f).
 * При завершении движения плавно проявляется обратно (animateTo 1f).
 */
@Composable
fun ProvidePagerScrollbarAlpha(
    pagerState: PagerState,
    content: @Composable () -> Unit
) {
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.isScrollInProgress || abs(pagerState.currentPageOffsetFraction) > 0.001f
        }.collect { isMoving ->
            if (isMoving) {
                alphaAnim.snapTo(0f)
            } else {
                alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 200))
            }
        }
    }

    val alphaProvider = remember { { alphaAnim.value } }

    CompositionLocalProvider(LocalScrollbarAlpha provides alphaProvider) {
        content()
    }
}

/**
 * Индикатор прокрутки: закрашивает ту долю дорожки, которая сейчас видна.
 *
 * [scrollPercent] — именно лямбда, а не готовая пара. Это принципиально.
 *
 * Позиция скролла меняется каждый кадр. Если передавать значение, вызывающий
 * обязан прочитать состояние у себя в теле, и тогда **весь экран**
 * перекомпонуется на каждом кадре прокрутки — отсюда ощущение «желейного»,
 * подтормаживающего скролла: главный поток вместо сдвига пикселей заново
 * собирает composable-дерево.
 *
 * С лямбдой чтение происходит внутри [Canvas], то есть в фазе отрисовки.
 * Смена значения инвалидирует только draw — ни рекомпозиции, ни новой
 * разметки. Вызывающий должен держать `State` и не разворачивать его через
 * `by`:
 *
 * ```
 * val scrollPercent = rememberVisibleRangePercent...(state)   // без by!
 * VerticalScrollbar { scrollPercent.value }
 *
 * // Scrollbar
 * Box( modifier = Modifier.fillMaxHeight()
 *                 .align(Alignment.CenterEnd).width(2.dp)) {
 *                     VerticalScrollbar { scrollPercent.value }
 *                 }
 *
 * ```
 *
 * `BoxWithConstraints` здесь раньше поднимал субкомпозицию ради высоты
 * дорожки. Не нужен: `DrawScope.size` даёт тот же размер бесплатно.
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
