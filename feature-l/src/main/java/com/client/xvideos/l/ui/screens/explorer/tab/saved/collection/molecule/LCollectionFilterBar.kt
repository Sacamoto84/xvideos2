package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.choice.ChoiceRow
import com.client.xvideos.common.ui.choice.ChoiceRowStyle
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionItemsOrder
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionTypeFilter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

private val TYPE_OPTIONS: ImmutableList<String> = LCollectionTypeFilter.entries.map { it.title }.toPersistentList()

/**
 * Над плитками открытой коллекции, одной строкой: отбор по типу (Все / Картинки /
 * Gif) рядом выбора в стиле C и значок сортировки с меню порядков. Значок
 * акцентный, пока выбран не порядок по умолчанию.
 */
@Composable
fun LCollectionFilterBar(
    typeFilter: LCollectionTypeFilter,
    order: LCollectionItemsOrder,
    onTypeChange: (LCollectionTypeFilter) -> Unit,
    onOrderChange: (LCollectionItemsOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val onSelectType = remember(onTypeChange) {
        { index: Int -> onTypeChange(LCollectionTypeFilter.entries[index]) }
    }
    Row(
        modifier = modifier
            .adaptiveContentWidth()
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChoiceRow(
            options = TYPE_OPTIONS,
            selectedIndex = typeFilter.ordinal,
            onSelectIndex = onSelectType,
            style = ChoiceRowStyle.TABS,
            accent = Theme.L.primaryColor,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Порядок: ${order.title}",
                    tint = if (order == LCollectionItemsOrder.NEWEST) Theme.L.textColor else Theme.L.primaryColor
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = Theme.L.grey3
            ) {
                val baseStyle = Theme.L.Type.menuItem
                LCollectionItemsOrder.entries.forEach { item ->
                    key(item.name) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    item.title,
                                    style = baseStyle.copy(color = if (item == order) Color.White else Theme.L.grey2)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onOrderChange(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun LCollectionFilterBarPreview() {
    LCollectionFilterBar(
        typeFilter = LCollectionTypeFilter.ALL,
        order = LCollectionItemsOrder.NEWEST,
        onTypeChange = {},
        onOrderChange = {}
    )
}
