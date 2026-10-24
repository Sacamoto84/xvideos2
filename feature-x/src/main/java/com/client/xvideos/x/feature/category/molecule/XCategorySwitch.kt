package com.client.xvideos.x.feature.category.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.feature.category.atom.XCategoryIcon

/**
 * Переключатель категории X у нижних вкладок: кнопка со значком текущей
 * категории, по нажатию — меню из трёх категорий.
 *
 * @param current Выбранная категория.
 * @param onSelect Выбор пункта меню, в том числе текущего.
 */
@Composable
fun XCategorySwitch(
    current: XCategory,
    onSelect: (XCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        // Без заливки: значок на фоне вкладок, кнопка не перетягивает внимание.
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            // Приглушён до яркости неактивных вкладок: кнопку жмут редко, белый светился.
            XCategoryIcon(category = current, modifier = Modifier.alpha(ICON_ALPHA))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF2A2A30),
        ) {
            XCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(text = category.title, color = Color.White) },
                    leadingIcon = { XCategoryIcon(category = category) },
                    onClick = {
                        expanded = false
                        onSelect(category)
                    },
                    modifier = if (category == current) Modifier.background(Color(0xFF3A3A42)) else Modifier,
                )
            }
        }
    }
}

private const val ICON_ALPHA = 0.5f

@Preview
@Composable
private fun XCategorySwitchPreview() {
    XCategorySwitch(current = XCategory.TRANS, onSelect = {})
}
