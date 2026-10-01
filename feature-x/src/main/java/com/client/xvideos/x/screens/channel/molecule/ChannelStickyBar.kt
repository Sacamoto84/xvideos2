package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.model.ChannelSortOrder
import com.client.xvideos.x.model.ChannelUiState

/**
 * Липкая панель под шапкой канала: сортировки, фильтр по моделям и кнопка
 * «Назад», когда шапка ушла вверх.
 *
 * @param showBackButton Показать «Назад»: кнопка шапки уже не видна.
 */
@Composable
fun ChannelStickyBar(
    uiState: ChannelUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSortChange: (ChannelSortOrder) -> Unit,
    onModelQueryChange: (String) -> Unit,
    onModelExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF040404))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = showBackButton,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            ChannelSortBar(
                selectedSort = uiState.currentSort,
                onSortChange = onSortChange,
                modifier = Modifier.weight(1f)
            )
        }

        if (uiState.hasModelFilters) {
            ChannelModelFilterBar(
                header = uiState.header,
                selectedModel = uiState.selectedModel,
                filteredModels = uiState.filteredModels,
                searchQuery = uiState.modelFilterQuery,
                isExpanded = uiState.isModelFilterExpanded,
                onQueryChange = onModelQueryChange,
                onExpandedChange = onModelExpandedChange,
                onSelectModel = onSelectModel,
                totalVideos = uiState.totalVideosCount.takeIf { it > 0 }
                    ?: uiState.header.videoCount,
            )
        }
    }
}

@Preview
@Composable
private fun ChannelStickyBarPreview() {
    ChannelStickyBar(
        uiState = ChannelUiState(
            header = ChannelHeaderModel(slug = "sample", name = "Sample Channel"),
        ),
        showBackButton = true,
        onBack = {},
        onSortChange = {},
        onModelQueryChange = {},
        onModelExpandedChange = {},
        onSelectModel = {},
    )
}
