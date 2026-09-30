package com.client.xvideos.r.ui.profile.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.ui.profile.atom.ExpandCollapseButton
import com.client.xvideos.r.ui.profile.atom.TagChip

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

@Preview
@Composable
private fun TagsBlockPreview() {
    TagsBlock(
        tags = listOf("Outdoor", "Amateur", "Verified", "Solo", "Big Assets", "POV", "4K"),
        tagsSelect = listOf("Verified"),
        onClick = {}
    )
}
