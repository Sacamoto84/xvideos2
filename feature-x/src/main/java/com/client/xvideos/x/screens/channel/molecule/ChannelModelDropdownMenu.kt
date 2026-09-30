package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.screens.channel.atom.ChannelModelDropdownItem

@Composable
fun ChannelModelDropdownMenu(
    isExpanded: Boolean,
    searchQuery: String,
    placeholderText: String,
    selectedModel: ChannelModelFilterItem?,
    filteredModels: List<ChannelModelFilterItem>,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { onExpandedChange(false) },
        modifier = modifier
            .widthIn(min = 270.dp, max = 320.dp)
            .heightIn(max = 380.dp)
            .background(Color(0xFF18181C))
            .border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(8.dp))
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = {
                Text(placeholderText, fontSize = 12.sp, color = Color(0xFF7E7E86))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFDE2600),
                unfocusedBorderColor = Color(0xFF333338),
                cursorColor = Color(0xFFDE2600),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(48.dp)
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = "Показать все видео",
                    fontSize = 13.sp,
                    fontWeight = if (selectedModel == null) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedModel == null) Color(0xFFDE2600) else Color.White
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = if (selectedModel == null) Color(0xFFDE2600) else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = {
                onSelectModel(null)
                onExpandedChange(false)
            }
        )

        HorizontalDivider(color = Color(0xFF26262B))

        if (filteredModels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Ничего не найдено", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            filteredModels.take(60).forEach { model ->
                ChannelModelDropdownItem(
                    model = model,
                    isSelected = selectedModel?.idUser == model.idUser,
                    onSelectModel = onSelectModel,
                    onExpandedChange = onExpandedChange,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChannelModelDropdownMenuPreview() {
    ChannelModelDropdownMenu(
        isExpanded = true,
        searchQuery = "",
        placeholderText = "Поиск модели...",
        selectedModel = null,
        filteredModels = listOf(
            ChannelModelFilterItem(idUser = 1L, displayName = "Jane Doe", gender = "Woman", fNbVideos = "12"),
            ChannelModelFilterItem(idUser = 2L, displayName = "John Smith", gender = "Man", fNbVideos = "5")
        ),
        onQueryChange = {},
        onExpandedChange = {},
        onSelectModel = {}
    )
}
