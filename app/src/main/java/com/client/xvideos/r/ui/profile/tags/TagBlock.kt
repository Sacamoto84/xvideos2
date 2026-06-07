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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TagsBlock(
    tags: List<String>,
    tagsSelect: List<String>,
    onClick: (String) -> Unit = {}
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    // Оптимизация 1: Мемоизация сортировки и сета для быстрого поиска
    val sortedTags = remember(tags) { tags.sorted() }
    val selectedSet = remember(tagsSelect) { tagsSelect.toSet() }
    
    // Оптимизация 2: Стабильная лямбда для предотвращения рекомпозиции чипов
    val currentOnClick by rememberUpdatedState(onClick)
    val stableOnClick = remember { { tag: String -> currentOnClick(tag) } }

    SubcomposeLayout { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val maxW = constraints.maxWidth

        // Измеряем кнопку сразу, она нам нужна для расчетов лимита
        val btnPl = subcompose("btn") {
            ExpandCollapseButton(expanded) { expanded = !expanded }
        }.first().measure(loose)

        val shownPlaceables = mutableListOf<Placeable>()
        var currentRowW = 0
        var currentLines = 1
        var isOverflow = false

        // Оптимизация 3: Subcompose только тех элементов, которые реально будут отображены
        for (tag in sortedTags) {
            val isSelected = tag in selectedSet
            
            // Предварительный замер (через subcompose только нужных)
            val p = subcompose(tag) {
                TagChip(tag, isSelected, stableOnClick)
            }.first().measure(loose)

            if (!expanded) {
                val needW = p.width + if (currentLines == 2) btnPl.width else 0
                if (currentRowW + needW > maxW) {
                    if (currentLines >= 2) {
                        isOverflow = true
                        break
                    }
                    currentLines++
                    currentRowW = 0
                }
            } else {
                if (currentRowW + p.width > maxW) {
                    currentRowW = 0
                }
            }
            
            shownPlaceables.add(p)
            currentRowW += p.width
        }

        // Добавляем кнопку в список отрисовки, если нужно
        if (expanded || isOverflow) {
            shownPlaceables.add(btnPl)
        }

        // Расчет итоговой высоты
        var totalHeight = 0
        var rowHeight = 0
        var xAcc = 0
        shownPlaceables.forEach { p ->
            if (xAcc + p.width > maxW) {
                totalHeight += rowHeight
                xAcc = 0
                rowHeight = 0
            }
            xAcc += p.width
            rowHeight = maxOf(rowHeight, p.height)
        }
        totalHeight += rowHeight

        layout(maxW, totalHeight) {
            var x = 0
            var y = 0
            var lineH = 0
            shownPlaceables.forEach { p ->
                if (x + p.width > maxW) {
                    x = 0
                    y += lineH
                    lineH = 0
                }
                p.placeRelative(x, y)
                x += p.width
                lineH = maxOf(lineH, p.height)
            }
        }
    }
}

@Composable
private fun TagChip(text: String, select: Boolean, onClick: (String) -> Unit) {
    // Оптимизация 4: Упрощение модификаторов и удаление лишних состояний
    Text(
        text = text,
        color = if (select) Color.Black else Color.White,
        fontSize = 14.sp,
        fontFamily = Theme.R.fontFamilyPopinsRegular,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp)) // Используем фиксированный радиус для скорости
            .background(if (select) Theme.R.colorYellow else Color.Transparent)
            .border(1.dp, Theme.R.colorYellow, RoundedCornerShape(16.dp))
            .clickable { onClick(text) }
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .wrapContentWidth()
    )
}

@Composable
private fun ExpandCollapseButton(expanded: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.dp, Theme.R.colorYellow, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.Close else Icons.Default.MoreHoriz,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}
