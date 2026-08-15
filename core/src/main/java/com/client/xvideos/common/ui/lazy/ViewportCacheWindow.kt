package com.client.xvideos.common.ui.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * Окно предзагрузки ленивого списка, заданное долей вьюпорта, а не фиксированными dp.
 *
 * `LazyLayoutCacheWindow(ahead, behind)` из foundation принимает Dp, и на сетках
 * с переключаемым числом колонок такая константа означает разное количество
 * элементов: при двух колонках 100.dp — это меньше половины ряда, при четырёх —
 * почти полтора ряда. Доля вьюпорта одинаково ведёт себя на любом экране и при
 * любой плотности сетки.
 *
 * `behind` здесь важен не меньше `ahead`: элементы позади окна не диспозятся, а
 * в этих сетках элемент — живое видео-превью со своим `ExoPlayer`. Без заднего
 * окна прокрутка вверх пересоздаёт плееры, которые только что отпустили.
 *
 * @param ahead доля вьюпорта, готовящаяся по направлению прокрутки.
 * @param behind доля вьюпорта, удерживаемая позади.
 */
@OptIn(ExperimentalFoundationApi::class)
fun viewportFractionCacheWindow(
    ahead: Float = 0.5f,
    behind: Float = 0.15f,
): LazyLayoutCacheWindow = ViewportFractionCacheWindow(ahead, behind)

@OptIn(ExperimentalFoundationApi::class)
private class ViewportFractionCacheWindow(
    private val ahead: Float,
    private val behind: Float,
) : LazyLayoutCacheWindow {

    override fun Density.calculateAheadWindow(viewport: Int): Int = (viewport * ahead).roundToInt()

    override fun Density.calculateBehindWindow(viewport: Int): Int = (viewport * behind).roundToInt()

    override fun equals(other: Any?): Boolean =
        other is ViewportFractionCacheWindow && other.ahead == ahead && other.behind == behind

    override fun hashCode(): Int = 31 * ahead.hashCode() + behind.hashCode()
}
