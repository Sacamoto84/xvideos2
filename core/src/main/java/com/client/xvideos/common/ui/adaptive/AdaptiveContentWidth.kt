package com.client.xvideos.common.ui.adaptive

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
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
    this then AdaptiveContentWidthElement(maxWidth)

/**
 * Ограничения для содержимого не шире [capPx] или `null`, если ограничивать
 * нечего: родитель не шире лимита или его ширина не ограничена.
 */
fun contentWidthConstraints(constraints: Constraints, capPx: Int): Constraints? =
    if (!constraints.hasBoundedWidth || constraints.maxWidth <= capPx) {
        null
    } else {
        constraints.copy(minWidth = minOf(constraints.minWidth, capPx), maxWidth = capPx)
    }

/** Отступ слева, при котором [contentPx] стоит по центру [availablePx]. */
fun centeredOffsetPx(availablePx: Int, contentPx: Int): Int =
    ((availablePx - contentPx) / 2).coerceAtLeast(0)

// data class: элементы с тем же лимитом равны, и Compose не трогает узел при
// перекомпоновке. Новый лимит обновляет узел, и тот перемеряется сам
// (shouldAutoInvalidate по умолчанию).
private data class AdaptiveContentWidthElement(val maxWidth: Dp) :
    ModifierNodeElement<AdaptiveContentWidthNode>() {

    override fun create() = AdaptiveContentWidthNode(maxWidth)

    override fun update(node: AdaptiveContentWidthNode) {
        node.maxWidth = maxWidth
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "adaptiveContentWidth"
        properties["maxWidth"] = maxWidth
    }
}

private class AdaptiveContentWidthNode(var maxWidth: Dp) : Modifier.Node(), LayoutModifierNode {

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val capped = contentWidthConstraints(constraints, maxWidth.roundToPx())
        return if (capped == null) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            val placeable = measurable.measure(capped)
            layout(constraints.maxWidth, placeable.height) {
                placeable.place(centeredOffsetPx(constraints.maxWidth, placeable.width), 0)
            }
        }
    }
}
