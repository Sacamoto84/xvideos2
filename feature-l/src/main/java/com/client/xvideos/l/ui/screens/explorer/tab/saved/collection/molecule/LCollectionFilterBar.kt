package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionTypeFilter
import com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.molecule.LikesFilterSegmentedRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private val TYPE_OPTIONS: ImmutableList<String> = LCollectionTypeFilter.entries.map { it.title }.toPersistentList()
private val ORDER_OPTIONS: ImmutableList<String> = persistentListOf("Сначала новые", "Сначала старые")

/**
 * Над плитками открытой коллекции: отбор по типу (Все / Картинки / Gif) и
 * порядок (сначала новые / сначала старые) — теми же переключателями, что на
 * «Лайках».
 */
@Composable
fun LCollectionFilterBar(
    typeFilter: LCollectionTypeFilter,
    newestFirst: Boolean,
    onTypeChange: (LCollectionTypeFilter) -> Unit,
    onNewestFirstChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSelectType = remember(onTypeChange) {
        { index: Int -> onTypeChange(LCollectionTypeFilter.entries[index]) }
    }
    val onSelectOrder = remember(onNewestFirstChange) {
        { index: Int -> onNewestFirstChange(index == 0) }
    }
    // Ряды — на ширину полосы шапки: сам по себе ряд Material3 сжимает кнопки
    // до самого длинного слова, и «Сначала новые» переносилось на две строки.
    Column(
        modifier = modifier
            .adaptiveContentWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LikesFilterSegmentedRow(
            options = TYPE_OPTIONS,
            selectedIndex = typeFilter.ordinal,
            onSelectIndex = onSelectType,
            topInset = 0.dp,
            modifier = Modifier.fillMaxWidth()
        )
        LikesFilterSegmentedRow(
            options = ORDER_OPTIONS,
            selectedIndex = if (newestFirst) 0 else 1,
            onSelectIndex = onSelectOrder,
            topInset = 0.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun LCollectionFilterBarPreview() {
    LCollectionFilterBar(
        typeFilter = LCollectionTypeFilter.ALL,
        newestFirst = true,
        onTypeChange = {},
        onNewestFirstChange = {}
    )
}
