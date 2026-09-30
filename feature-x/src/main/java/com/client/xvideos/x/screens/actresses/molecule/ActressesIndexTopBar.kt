package com.client.xvideos.x.screens.actresses.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ActressesIndexCatalog
import com.client.xvideos.x.model.ActressesIndexDropdownType
import com.client.xvideos.x.model.ActressesIndexFilterOption

@Composable
fun ActressesIndexTopBar(
    title: String,
    subtitle: String,
    topCutout: Dp,
    catalog: ActressesIndexCatalog,
    activeDropdown: ActressesIndexDropdownType?,
    searchQuery: String,
    onBack: () -> Unit,
    onToggleDropdown: (ActressesIndexDropdownType) -> Unit,
    onSelectOption: (ActressesIndexFilterOption) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseDropdown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0B0E))
            .padding(top = topCutout)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title.ifBlank { "Каталог актрис" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        ActressesFilterBar(
            catalog = catalog,
            activeDropdown = activeDropdown,
            searchQuery = searchQuery,
            onToggleDropdown = onToggleDropdown,
            onSelectOption = onSelectOption,
            onSearchQueryChange = onSearchQueryChange,
            onCloseDropdown = onCloseDropdown,
        )
    }
}

@Preview
@Composable
private fun ActressesIndexTopBarPreview() {
    ActressesIndexTopBar(
        title = "Каталог моделей",
        subtitle = "Топ рейтинги",
        topCutout = 0.dp,
        catalog = ActressesIndexCatalog(),
        activeDropdown = null,
        searchQuery = "",
        onBack = {},
        onToggleDropdown = {},
        onSelectOption = {},
        onSearchQueryChange = {},
        onCloseDropdown = {}
    )
}
