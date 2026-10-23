package com.client.xvideos.x.screens.channel.model

import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Ограничения замера шапки: ширина экрана, высота без потолка.
 *
 * Шапка бывает выше экрана — у модели с длинным сотрудничеством. С потолком в
 * высоту экрана она обрезалась, диапазон схлопывания упирался в экран, и ниже
 * обреза было не долистать. Без потолка шапка уезжает вверх целиком.
 */
internal fun channelHeaderConstraints(parent: Constraints): Constraints =
    parent.copy(minHeight = 0, maxHeight = Constraints.Infinity)

/**
 * Доводить ли шапку до края после отпускания.
 *
 * Доводка оставляет шапку только раскрытой или схлопнутой. Пока путь
 * схлопывания [collapseRangePx] не длиннее окна под вырезом [viewportRangePx],
 * раскрытая шапка видна целиком и доводка ничего не прячет. Шапку выше окна
 * нужно уметь остановить посередине — её листают с обычной инерцией.
 */
internal fun channelHeaderSnaps(collapseRangePx: Float, viewportRangePx: Float): Boolean =
    collapseRangePx <= viewportRangePx

/**
 * Вертикальная раскладка экрана канала при текущем смещении шапки.
 *
 * @property headerY Верх шапки: от верха экрана, баннер заходит под вырез камеры.
 * @property stickyY Верх липкой панели: не выше низа выреза.
 * @property pagerY Верх пейджера страниц.
 * @property coverAlpha Непрозрачность плашки выреза: 0 — плашки нет, видна шапка.
 */
internal data class ChannelCollapsingGeometry(
    val headerY: Int,
    val stickyY: Int,
    val pagerY: Int,
    val coverAlpha: Float,
)

/**
 * Раскладка экрана канала: шапка от верха экрана, липкая панель упирается в низ
 * выреза [topInsetPx].
 *
 * Плашка выреза проявляется на последнем отрезке длиной в вырез, пока панель
 * подходит к нему, и закрывает уехавшую под вырез шапку, когда панель прилипла.
 * У раскрытой шапки плашки нет: под вырезом виден баннер.
 *
 * @param headerOffsetPx Смещение шапки: 0 — раскрыта, отрицательное — уехала вверх.
 */
internal fun channelCollapsingGeometry(
    headerOffsetPx: Float,
    headerHeight: Int,
    stickyBarHeight: Int,
    topInsetPx: Int,
): ChannelCollapsingGeometry {
    val headerY = headerOffsetPx.roundToInt()
    val stickyY = (headerY + headerHeight).coerceAtLeast(topInsetPx)
    val coverAlpha = if (topInsetPx > 0) {
        (1f - (stickyY - topInsetPx).toFloat() / topInsetPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    return ChannelCollapsingGeometry(
        headerY = headerY,
        stickyY = stickyY,
        pagerY = stickyY + stickyBarHeight,
        coverAlpha = coverAlpha,
    )
}
