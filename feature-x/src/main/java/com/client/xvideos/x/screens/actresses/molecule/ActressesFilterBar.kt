package com.client.xvideos.x.screens.actresses.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ActressesIndexCatalog
import com.client.xvideos.x.model.ActressesIndexDropdownType
import com.client.xvideos.x.model.ActressesIndexFilterOption
import com.client.xvideos.x.screens.actresses.atom.FilterButton

/**
 * Панель фильтров каталога актрис с 3 кнопками выпадающих списков
 * (Регион / Страна, Тип моделей, Период / Сортировка).
 */
@Composable
fun ActressesFilterBar(
    catalog: ActressesIndexCatalog,
    activeDropdown: ActressesIndexDropdownType?,
    searchQuery: String,
    onToggleDropdown: (ActressesIndexDropdownType) -> Unit,
    onSelectOption: (ActressesIndexFilterOption) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseDropdown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0C))
            .padding(vertical = 6.dp)
    ) {
        // Горизонтальный ряд кнопок фильтров
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterButton(
                title = catalog.geoFilter.displayTitle,
                isExpanded = activeDropdown == ActressesIndexDropdownType.GEO,
                onClick = { onToggleDropdown(ActressesIndexDropdownType.GEO) }
            )

            FilterButton(
                title = catalog.profileTypeFilter.displayTitle,
                isExpanded = activeDropdown == ActressesIndexDropdownType.PROFILE_TYPE,
                onClick = { onToggleDropdown(ActressesIndexDropdownType.PROFILE_TYPE) }
            )

            FilterButton(
                title = catalog.timeSortFilter.displayTitle,
                isExpanded = activeDropdown == ActressesIndexDropdownType.TIME_SORT,
                onClick = { onToggleDropdown(ActressesIndexDropdownType.TIME_SORT) }
            )
        }

        // Выпадающее меню с опциями
        AnimatedVisibility(
            visible = activeDropdown != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val group = when (activeDropdown) {
                ActressesIndexDropdownType.GEO -> catalog.geoFilter
                ActressesIndexDropdownType.PROFILE_TYPE -> catalog.profileTypeFilter
                ActressesIndexDropdownType.TIME_SORT -> catalog.timeSortFilter
                null -> null
            }

            if (group != null) {
                DropdownOptionsPanel(
                    group = group,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSelectOption = onSelectOption,
                    onClose = onCloseDropdown,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ActressesFilterBarPreview() {
    ActressesFilterBar(
        catalog = ActressesIndexCatalog(),
        activeDropdown = null,
        searchQuery = "",
        onToggleDropdown = {},
        onSelectOption = {},
        onSearchQueryChange = {},
        onCloseDropdown = {}
    )
}
