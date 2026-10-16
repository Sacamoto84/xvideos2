package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.choice.ChoiceRow
import com.client.xvideos.common.ui.choice.ChoiceRowStyle
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionItemsOrder
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionTypeFilter
import com.client.xvideos.l.ui.screens.explorer.tab.saved.molecule.LSortMenuButton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

private val TYPE_OPTIONS: ImmutableList<String> = LCollectionTypeFilter.entries.map { it.title }.toPersistentList()
private val ORDER_OPTIONS: ImmutableList<String> = LCollectionItemsOrder.entries.map { it.title }.toPersistentList()

/**
 * Над плитками открытой коллекции, одной строкой: слева сортировка — значок и
 * выбранный порядок одной кнопкой, как в списке коллекций; справа отбор по типу
 * (Все / Картинки / Gif) рядом выбора в стиле C.
 */
@Composable
fun LCollectionFilterBar(
    typeFilter: LCollectionTypeFilter,
    order: LCollectionItemsOrder,
    onTypeChange: (LCollectionTypeFilter) -> Unit,
    onOrderChange: (LCollectionItemsOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSelectType = remember(onTypeChange) {
        { index: Int -> onTypeChange(LCollectionTypeFilter.entries[index]) }
    }
    val onSelectOrder = remember(onOrderChange) {
        { index: Int -> onOrderChange(LCollectionItemsOrder.entries[index]) }
    }
    Row(
        modifier = modifier
            .adaptiveContentWidth()
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LSortMenuButton(
            options = ORDER_OPTIONS,
            selectedIndex = order.ordinal,
            onSelectIndex = onSelectOrder,
            contentDescription = "Порядок элементов"
        )
        ChoiceRow(
            options = TYPE_OPTIONS,
            selectedIndex = typeFilter.ordinal,
            onSelectIndex = onSelectType,
            style = ChoiceRowStyle.TABS,
            accent = Theme.L.primaryColor,
            modifier = Modifier.weight(1f)
        )
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
