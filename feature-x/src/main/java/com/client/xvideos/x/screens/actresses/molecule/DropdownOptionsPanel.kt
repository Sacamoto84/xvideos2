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
import com.client.xvideos.x.screens.actresses.atom.DropdownOptionRow
import com.client.xvideos.x.screens.actresses.atom.DropdownOptionsSearchField

@Composable
fun DropdownOptionsPanel(
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
        searchQuery = "",
        onSearchQueryChange = {},
        onSelectOption = {},
        onClose = {}
    )
}
