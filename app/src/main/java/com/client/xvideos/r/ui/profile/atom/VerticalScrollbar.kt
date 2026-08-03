package com.client.xvideos.r.ui.profile.atom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

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
 * ```
 *
 * `BoxWithConstraints` здесь раньше поднимал субкомпозицию ради высоты
 * дорожки. Не нужен: `DrawScope.size` даёт тот же размер бесплатно.
 */
@Composable
fun VerticalScrollbar(scrollPercent: () -> Pair<Float, Float>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
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
                color = Color.Gray,
                topLeft = Offset(x = 0f, y = indicatorOffsetY),
                size = Size(width = size.width, height = indicatorHeight)
            )
        }
    }
}
