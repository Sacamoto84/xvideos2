package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.screens.channel.atom.ChannelModelTriggerButton

/**
 * Строка фильтрации и поиска по моделям/актрисам канала (или каналам модели).
 *
 * Отображает счётчик доступных видеороликов слева и компактное поле поиска с выпадающим
 * списком моделей, иконками пола и счётчиками справа.
 */
@Composable
fun ChannelModelFilterBar(
    header: ChannelHeaderModel,
    selectedModel: ChannelModelFilterItem?,
    filteredModels: List<ChannelModelFilterItem>,
    searchQuery: String,
    isExpanded: Boolean,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
    totalVideos: Int,
    modifier: Modifier = Modifier,
) {
    val isModelProfile = header.isModel
    val placeholderText = if (isModelProfile) "Поиск канала..." else "Поиск модели..."

    val summaryText = if (selectedModel != null) {
        val count = selectedModel.countText.ifBlank { totalVideos.toString() }
        "$count видео с ${selectedModel.displayName}"
    } else {
        val count = header.videoCount.takeIf { it > 0 } ?: totalVideos
        "$count видео"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Текст со счётчиком видео
        Text(
            text = summaryText,
            color = if (selectedModel != null) Color.White else Color(0xFF9E9EA4),
            fontSize = 13.sp,
            fontWeight = if (selectedModel != null) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        // Поле-триггер выпадающего поиска модели
        Box {
            ChannelModelTriggerButton(
                selectedModel = selectedModel,
                isExpanded = isExpanded,
                placeholderText = placeholderText,
                onExpandedChange = onExpandedChange,
                onSelectModel = onSelectModel
            )

            ChannelModelDropdownMenu(
                isExpanded = isExpanded,
                searchQuery = searchQuery,
                placeholderText = placeholderText,
                selectedModel = selectedModel,
                filteredModels = filteredModels,
                onQueryChange = onQueryChange,
                onExpandedChange = onExpandedChange,
                onSelectModel = onSelectModel
            )
        }
    }
}

@Preview
@Composable
private fun ChannelModelFilterBarPreview() {
    ChannelModelFilterBar(
        header = ChannelHeaderModel(name = "Sample", videoCount = 42),
        selectedModel = null,
        filteredModels = emptyList(),
        searchQuery = "",
        isExpanded = false,
        onQueryChange = {},
        onExpandedChange = {},
        onSelectModel = {},
        totalVideos = 42
    )
}
