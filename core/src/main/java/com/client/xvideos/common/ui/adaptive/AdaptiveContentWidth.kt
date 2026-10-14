package com.client.xvideos.common.ui.adaptive

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Содержимое не шире [maxWidth] и по центру — только если родитель шире.
 *
 * Пока родитель не шире лимита (телефон, внешний экран складного), модификатор
 * ничего не меняет. Когда шире — узел занимает всю ширину родителя, а
 * содержимое меряется не шире [maxWidth] и ставится по центру. Поэтому фон,
 * заданный в цепочке **до** этого модификатора, остаётся на всю ширину.
 */
fun Modifier.adaptiveContentWidth(maxWidth: Dp = CONTENT_MAX_WIDTH_DP.dp): Modifier =
    layout { measurable, constraints ->
        val capPx = maxWidth.roundToPx()
        if (!constraints.hasBoundedWidth || constraints.maxWidth <= capPx) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            val placeable = measurable.measure(
                constraints.copy(minWidth = minOf(constraints.minWidth, capPx), maxWidth = capPx)
            )
            layout(constraints.maxWidth, placeable.height) {
                placeable.place(centeredOffsetPx(constraints.maxWidth, placeable.width), 0)
            }
        }
    }

/** Отступ слева, при котором [contentPx] стоит по центру [availablePx]. */
fun centeredOffsetPx(availablePx: Int, contentPx: Int): Int =
    ((availablePx - contentPx) / 2).coerceAtLeast(0)
