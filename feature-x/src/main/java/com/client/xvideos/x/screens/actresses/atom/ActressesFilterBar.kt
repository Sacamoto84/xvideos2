package com.client.xvideos.x.screens.actresses.atom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ActressesIndexCatalog
import com.client.xvideos.x.model.ActressesIndexDropdownType
import com.client.xvideos.x.model.ActressesIndexFilterGroup
import com.client.xvideos.x.model.ActressesIndexFilterOption

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

@Composable
private fun FilterButton(
    title: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isExpanded) Color(0xFFB01E00) else Color(0xFFDE2600)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (isExpanded) "▲" else "▼",
            fontSize = 9.sp,
            color = Color.White
        )
    }
}

@Composable
private fun DropdownOptionsPanel(
    group: ActressesIndexFilterGroup,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectOption: (ActressesIndexFilterOption) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showSearch = group.type == ActressesIndexDropdownType.GEO || group.type == ActressesIndexDropdownType.PROFILE_TYPE
    val filteredOptions = if (searchQuery.isBlank()) {
        group.options
    } else {
        group.options.filter { it.matches(searchQuery) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF33333A), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // Шапка выпадающего списка
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = group.displayTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp)
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showSearch) {
            Spacer(modifier = Modifier.height(4.dp))
            DropdownOptionsSearchField(
                groupType = group.type,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Список опций
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (filteredOptions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ничего не найдено",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            } else {
                filteredOptions.forEach { option ->
                    DropdownOptionRow(option = option, onSelectOption = onSelectOption)
                }
            }
        }
    }
}

@Composable
private fun DropdownOptionsSearchField(
    groupType: ActressesIndexDropdownType,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                text = if (groupType == ActressesIndexDropdownType.GEO) "Поиск страны..." else "Поиск типа профиля...",
                fontSize = 12.sp,
                color = Color.Gray
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        },
        trailingIcon = if (searchQuery.isNotBlank()) {
            {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Очистить",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else null,
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1F1F24),
            unfocusedContainerColor = Color(0xFF1A1A1E),
            focusedIndicatorColor = Color(0xFFDE2600),
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    )
}

@Composable
private fun DropdownOptionRow(
    option: ActressesIndexFilterOption,
    onSelectOption: (ActressesIndexFilterOption) -> Unit
) {
    val isSelected = option.isActive
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF2A1518) else Color.Transparent)
            .clickable { onSelectOption(option) }
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(
            text = option.title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFDE2600) else Color(0xFFE0E0E0),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFFDE2600),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
