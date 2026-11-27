package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.client.xvideos.R

private val SETTINGS_GROUP_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp)

private val SettingsGroupBackground: @Composable () -> Unit = {
    Box(Modifier.background(SettingsCardColor))
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        contents = listOf(
            SettingsGroupBackground,
            { CompositionLocalProvider(LocalSettingsInGroup provides true) { content() } }
        ),
        modifier = modifier.then(SETTINGS_GROUP_BASE_MODIFIER),
        measurePolicy = SettingsGroupMeasurePolicy
    )
}

private class SettingsGroupRow(val placeable: Placeable, val y: Int, val shape: Shape)

private data class SettingsGroupBlock(
    val top: Float,
    val bottom: Float,
    val topCorner: Float,
    val bottomCorner: Float
)

private data class SettingsGroupBackgroundShape(val blocks: List<SettingsGroupBlock>) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        blocks.forEach { block ->
            val top = CornerRadius(block.topCorner)
            val bottom = CornerRadius(block.bottomCorner)
            path.addRoundRect(
                RoundRect(
                    left = 0f,
                    top = block.top,
                    right = size.width,
                    bottom = block.bottom,
                    topLeftCornerRadius = top,
                    topRightCornerRadius = top,
                    bottomRightCornerRadius = bottom,
                    bottomLeftCornerRadius = bottom
                )
            )
        }
        return Outline.Generic(path)
    }
}

private object SettingsGroupMeasurePolicy : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints
    ): MeasureResult {
        val blocks = measureBlocks(measurables[1], constraints.copy(minWidth = 0, minHeight = 0))
        val gap = SettingsGroupGapHeight.roundToPx()
        val rows = ArrayList<SettingsGroupRow>()
        val blockBounds = ArrayList<SettingsGroupBlock>(blocks.size)
        var y = 0
        blocks.forEachIndexed { blockIndex, block ->
            if (blockIndex > 0) y += gap
            val topCorner = if (blockIndex == 0) SettingsCardCorner else SettingsGroupInnerCorner
            val bottomCorner = if (blockIndex == blocks.lastIndex) SettingsCardCorner else SettingsGroupInnerCorner
            val blockTop = y
            block.forEachIndexed { rowIndex, placeable ->
                val rowTop = if (rowIndex == 0) topCorner else 0.dp
                val rowBottom = if (rowIndex == block.lastIndex) bottomCorner else 0.dp
                val shape = RoundedCornerShape(
                    topStart = rowTop,
                    topEnd = rowTop,
                    bottomEnd = rowBottom,
                    bottomStart = rowBottom
                )
                rows += SettingsGroupRow(placeable, y, shape)
                y += placeable.height
            }
            blockBounds += SettingsGroupBlock(blockTop.toFloat(), y.toFloat(), topCorner.toPx(), bottomCorner.toPx())
        }
        val width = constraints.constrainWidth(rows.maxOfOrNull { it.placeable.width } ?: 0)
        val height = constraints.constrainHeight(y)
        val background = measurables[0].first().measure(Constraints.fixed(width, height))
        val backgroundShape = SettingsGroupBackgroundShape(blockBounds)
        return layout(width, height) {
            background.placeWithLayer(0, 0) {
                shape = backgroundShape
                clip = true
            }
            rows.forEach { row ->
                row.placeable.placeWithLayer(0, row.y) {
                    shape = row.shape
                    clip = true
                }
            }
        }
    }

    private fun measureBlocks(measurables: List<Measurable>, constraints: Constraints): List<List<Placeable>> {
        val blocks = ArrayList<MutableList<Placeable>>()
        var startNewBlock = true
        measurables.forEach { measurable ->
            if (measurable.layoutId == SettingsGroupGap) {
                startNewBlock = true
            } else {
                val placeable = measurable.measure(constraints)
                if (startNewBlock) {
                    blocks += mutableListOf(placeable)
                } else {
                    blocks.last() += placeable
                }
                startNewBlock = false
            }
        }
        return blocks
    }
}

@Preview
@Composable
private fun SettingsGroupPreview() {
    SettingsGroup {
        SettingsListItem(
            icon = R.drawable.icon_red,
            text = "Первый пункт",
            subtitle = "Описание первого пункта"
        )
        SettingsDivider()
        SettingsListItem(
            icon = R.drawable.icon_red,
            text = "Второй пункт",
            subtitle = "Описание второго пункта"
        )
    }
}
