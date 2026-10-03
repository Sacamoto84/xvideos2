package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.screens.channel.atom.ChannelModelDropdownItem

/**
 * Выпадающий список моделей канала (или каналов модели) для фильтра видео.
 *
 * Поиска нет: модель выбирают из списка, поэтому в нём все модели, а не первые
 * десятки. У крупной студии их сотни — список ленивый. Ширина у него фиксированная:
 * меню меряет содержимое по intrinsic-ширине, а ленивый список её не отдаёт.
 */
@Composable
fun ChannelModelDropdownMenu(
    isExpanded: Boolean,
    selectedModel: ChannelModelFilterItem?,
    models: List<ChannelModelFilterItem>,
    onExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { onExpandedChange(false) },
        modifier = modifier
            .background(Color(0xFF18181C))
            .border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(8.dp))
    ) {
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

        // Меню открывается на выбранной модели, а не в начале списка.
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = models
                .indexOfFirst { it.idUser == selectedModel?.idUser }
                .coerceAtLeast(0)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .width(280.dp)
                .heightIn(max = 300.dp)
        ) {
            items(models) { model ->
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
        selectedModel = null,
        models = listOf(
            ChannelModelFilterItem(idUser = 1L, displayName = "Jane Doe", gender = "Woman", fNbVideos = "12"),
            ChannelModelFilterItem(idUser = 2L, displayName = "John Smith", gender = "Man", fNbVideos = "5")
        ),
        onExpandedChange = {},
        onSelectModel = {}
    )
}
