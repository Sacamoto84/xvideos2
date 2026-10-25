package com.client.xvideos.xlive.ui.search.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.xlive.model.XLiveAgeRange
import com.client.xvideos.xlive.model.XLivePrivatePrice
import com.client.xvideos.xlive.model.XLiveRegion
import com.client.xvideos.xlive.model.XLiveSearchFilters
import com.client.xvideos.xlive.ui.search.atom.XLiveFilterChip
import com.client.xvideos.xlive.ui.search.atom.XLiveSearchField
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private val ageTitles: ImmutableList<String> = XLiveAgeRange.entries.map { it.title }.toImmutableList()
private val regionTitles: ImmutableList<String> = XLiveRegion.entries.map { it.title }.toImmutableList()
private val priceTitles: ImmutableList<String> =
    XLivePrivatePrice.entries.map { if (it == XLivePrivatePrice.ANY) it.title else "${it.title} ток/мин" }.toImmutableList()

/**
 * Шапка «Поиска»: поле ника или слова из темы и под ним фильтры — возраст,
 * регион, цена привата. Фильтры переносятся на следующую строку, если не
 * влезают в ширину.
 *
 * Фон — на всю ширину, поле и фильтры — с лимитом ширины по центру, после
 * фона, как шапки экранов раздела.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XLiveSearchHeader(
    query: String,
    filters: XLiveSearchFilters,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onFilters: (XLiveSearchFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0B0E))
            .adaptiveContentWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        XLiveSearchField(query = query, onQueryChange = onQueryChange, onSearch = onSearch, onClear = onClear)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        ) {
            XLiveFilterChip(
                label = "Возраст",
                options = ageTitles,
                selected = filters.age.ordinal,
                onSelect = { onFilters(filters.copy(age = XLiveAgeRange.entries[it])) },
            )
            XLiveFilterChip(
                label = "Регион",
                options = regionTitles,
                selected = filters.region.ordinal,
                onSelect = { onFilters(filters.copy(region = XLiveRegion.entries[it])) },
            )
            XLiveFilterChip(
                label = "Приват",
                options = priceTitles,
                selected = filters.price.ordinal,
                onSelect = { onFilters(filters.copy(price = XLivePrivatePrice.entries[it])) },
            )
        }
    }
}

@Preview
@Composable
private fun XLiveSearchHeaderPreview() {
    XLiveSearchHeader(
        query = "anna",
        filters = XLiveSearchFilters(age = XLiveAgeRange.AGE_18_21),
        onQueryChange = {},
        onSearch = {},
        onClear = {},
        onFilters = {},
    )
}
