package com.client.xvideos.x.screens.actresses.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ActressesIndexDropdownType
import com.client.xvideos.x.model.ActressesIndexFilterGroup
import com.client.xvideos.x.model.ActressesIndexFilterOption
import com.client.xvideos.x.model.CatalogDropdownSearch
import com.client.xvideos.x.model.CatalogProfileHit
import com.client.xvideos.x.model.CatalogProfileKind
import com.client.xvideos.x.screens.actresses.atom.CatalogProfileHitRow
import com.client.xvideos.x.screens.actresses.atom.DropdownOptionRow
import com.client.xvideos.x.screens.actresses.atom.DropdownOptionsSearchField
import com.client.xvideos.x.screens.actresses.atom.DropdownSearchMessage

/**
 * Открытый выпадающий список каталога.
 *
 * Список стран ищет по всем странам каталога ([countryCount] — сколько их),
 * второй список — модели или каналы по имени ([profileKind]). Пока запрос пуст,
 * виден сам список; с запросом — найденное.
 */
@Composable
fun DropdownOptionsPanel(
    group: ActressesIndexFilterGroup,
    profileKind: CatalogProfileKind,
    countryCount: Int,
    search: CatalogDropdownSearch,
    onSearchQueryChange: (String) -> Unit,
    onSelectOption: (ActressesIndexFilterOption) -> Unit,
    onProfileClick: (CatalogProfileHit) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGeo = group.type == ActressesIndexDropdownType.GEO
    val showSearch = group.type == ActressesIndexDropdownType.PROFILE_TYPE || (isGeo && countryCount > 0)
    val isSearching = showSearch && search.query.isNotBlank()
    val placeholder = when {
        isGeo -> "Поиск страны..."
        profileKind == CatalogProfileKind.CHANNELS -> "Поиск канала..."
        else -> "Поиск профиля..."
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
                placeholder = placeholder,
                searchQuery = search.query,
                onSearchQueryChange = onSearchQueryChange
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Список опций или найденное
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                !isSearching -> if (group.options.isEmpty()) {
                    DropdownSearchMessage(text = "Ничего не найдено")
                } else {
                    group.options.forEach { option ->
                        DropdownOptionRow(option = option, onSelectOption = onSelectOption)
                    }
                }
                isGeo -> if (search.countryHits.isEmpty()) {
                    DropdownSearchMessage(text = "Ничего не найдено")
                } else {
                    search.countryHits.forEach { option ->
                        DropdownOptionRow(option = option, onSelectOption = onSelectOption)
                    }
                }
                search.profileHits.isNotEmpty() -> search.profileHits.forEach { hit ->
                    CatalogProfileHitRow(hit = hit, onClick = onProfileClick)
                }
                search.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFDE2600), strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
                else -> DropdownSearchMessage(text = search.error ?: "Ничего не найдено")
            }
        }
    }
}

@Preview
@Composable
private fun DropdownOptionsPanelPreview() {
    DropdownOptionsPanel(
        group = ActressesIndexFilterGroup(
            activeTitle = "Страны",
            options = listOf(
                ActressesIndexFilterOption(title = "Россия", urlPath = "/from/russia", isActive = true),
                ActressesIndexFilterOption(title = "Бразилия", urlPath = "/from/brazil", isActive = false)
            )
        ),
        profileKind = CatalogProfileKind.MODELS,
        countryCount = 0,
        search = CatalogDropdownSearch(),
        onSearchQueryChange = {},
        onSelectOption = {},
        onProfileClick = {},
        onClose = {}
    )
}
