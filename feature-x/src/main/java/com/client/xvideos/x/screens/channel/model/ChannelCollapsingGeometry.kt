package com.client.xvideos.x.screens.channel.model

import kotlin.math.roundToInt

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
