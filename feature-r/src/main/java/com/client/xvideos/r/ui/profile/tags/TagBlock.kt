package com.client.xvideos.r.ui.profile.tags

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TAG_CHIP_CORNER = 16.dp
private val TAG_CHIP_SHAPE = RoundedCornerShape(TAG_CHIP_CORNER)
private val TAG_CHIP_HEIGHT = 32.dp
private val TAG_CHIP_BORDER_WIDTH = 1.dp
private val TAG_CHIP_OUTER_HORIZONTAL_PADDING = 4.dp
private val TAG_CHIP_OUTER_VERTICAL_PADDING = 2.dp
private val TAG_CHIP_INNER_HORIZONTAL_PADDING = 12.dp
private val TAG_CHIP_INNER_VERTICAL_PADDING = 4.dp
private val TAG_CHIP_FONT_SIZE = 14.sp

private val EXPAND_BUTTON_SIZE = 32.dp
private val EXPAND_ICON_SIZE = 18.dp
private val EXPAND_BUTTON_BORDER_WIDTH = 1.dp

private const val CD_EXPAND_TAGS = "Развернуть теги"
private const val CD_COLLAPSE_TAGS = "Свернуть теги"

private val TAG_CHIP_BASE_MODIFIER = Modifier
    .padding(horizontal = TAG_CHIP_OUTER_HORIZONTAL_PADDING, vertical = TAG_CHIP_OUTER_VERTICAL_PADDING)
    .height(TAG_CHIP_HEIGHT)
    .clip(TAG_CHIP_SHAPE)

private val TAG_CHIP_CONTENT_PADDING_MODIFIER = Modifier
    .padding(horizontal = TAG_CHIP_INNER_HORIZONTAL_PADDING, vertical = TAG_CHIP_INNER_VERTICAL_PADDING)
    .wrapContentWidth()

private val EXPAND_BUTTON_BASE_MODIFIER = Modifier
    .padding(horizontal = TAG_CHIP_OUTER_HORIZONTAL_PADDING, vertical = TAG_CHIP_OUTER_VERTICAL_PADDING)
    .size(EXPAND_BUTTON_SIZE)
    .clip(CircleShape)
    .background(Color.Transparent)

private val EXPAND_ICON_MODIFIER = Modifier.size(EXPAND_ICON_SIZE)

@Composable
fun TagsBlock(
    tags: List<String>,
    tagsSelect: List<String>,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit = {}
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    // Оптимизация 1: Мемоизация сортировки и сета для быстрого поиска
    val sortedTags = remember(tags) { tags.sorted() }
    val selectedSet = remember(tagsSelect) { tagsSelect.toSet() }
    
    // Оптимизация 2: Стабильная лямбда для предотвращения рекомпозиции чипов
    val currentOnClick by rememberUpdatedState(onClick)
    val stableOnClick = remember { { tag: String -> currentOnClick(tag) } }
    val onToggleExpanded: () -> Unit = remember { { expanded = !expanded } }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val maxW = constraints.maxWidth

        // Измеряем кнопку сразу, она нам нужна для расчетов лимита
        val buttonPlaceable = subcompose("btn") {
            ExpandCollapseButton(expanded, onToggleExpanded)
        }.first().measure(loose)

        val shownPlaceables = mutableListOf<Placeable>()
        var currentRowW = 0
        var currentLines = 1
        var isOverflow = false

        // Оптимизация 3: Subcompose только тех элементов, которые реально будут отображены
        for (tag in sortedTags) {
            val isSelected = tag in selectedSet
            
            // Предварительный замер (через subcompose только нужных)
            val placeable = subcompose(tag) {
                TagChip(tag, isSelected, stableOnClick)
            }.first().measure(loose)

            if (!expanded) {
                val needW = placeable.width + if (currentLines == 2) buttonPlaceable.width else 0
                if (currentRowW + needW > maxW) {
                    if (currentLines >= 2) {
                        isOverflow = true
                        break
                    }
                    currentLines++
                    currentRowW = 0
                }
            } else {
                if (currentRowW + placeable.width > maxW) {
                    currentRowW = 0
                }
            }
            
            shownPlaceables.add(placeable)
            currentRowW += placeable.width
        }

        // Добавляем кнопку в список отрисовки, если нужно
        if (expanded || isOverflow) {
            shownPlaceables.add(buttonPlaceable)
        }

        // Расчет итоговой высоты
        var totalHeight = 0
        var rowHeight = 0
        var xAcc = 0
        shownPlaceables.forEach { placeable ->
            if (xAcc + placeable.width > maxW) {
                totalHeight += rowHeight
                xAcc = 0
                rowHeight = 0
            }
            xAcc += placeable.width
            rowHeight = maxOf(rowHeight, placeable.height)
        }
        totalHeight += rowHeight

        layout(maxW, totalHeight) {
            var x = 0
            var y = 0
            var lineH = 0
            shownPlaceables.forEach { placeable ->
                if (x + placeable.width > maxW) {
                    x = 0
                    y += lineH
                    lineH = 0
                }
                placeable.placeRelative(x, y)
                x += placeable.width
                lineH = maxOf(lineH, placeable.height)
            }
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    select: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleTagClick = remember(text, onClick) { { onClick(text) } }
    Text(
        text = text,
        color = if (select) Color.Black else Color.White,
        fontSize = TAG_CHIP_FONT_SIZE,
        fontFamily = Theme.R.fontFamilyPopinsRegular,
        modifier = modifier
            .then(TAG_CHIP_BASE_MODIFIER)
            .background(if (select) Theme.R.colorYellow else Color.Transparent)
            .border(TAG_CHIP_BORDER_WIDTH, Theme.R.colorYellow, TAG_CHIP_SHAPE)
            .clickable(onClick = handleTagClick)
            .then(TAG_CHIP_CONTENT_PADDING_MODIFIER)
    )
}

@Composable
private fun ExpandCollapseButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(EXPAND_BUTTON_BASE_MODIFIER)
            .border(EXPAND_BUTTON_BORDER_WIDTH, Theme.R.colorYellow, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.Close else Icons.Default.MoreHoriz,
            contentDescription = if (expanded) CD_COLLAPSE_TAGS else CD_EXPAND_TAGS,
            tint = Color.White,
            modifier = EXPAND_ICON_MODIFIER
        )
    }
}

@Preview
@Composable
private fun TagsBlockPreview() {
    TagsBlock(
        tags = listOf("Outdoor", "Amateur", "Verified", "Solo", "Big Assets", "POV", "4K"),
        tagsSelect = listOf("Verified"),
        onClick = {}
    )
}
