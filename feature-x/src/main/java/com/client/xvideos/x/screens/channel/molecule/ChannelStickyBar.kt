package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.model.ChannelSortOrder
import com.client.xvideos.x.model.ChannelUiState

/**
 * Липкая панель под шапкой канала: сортировки и фильтр по моделям.
 */
@Composable
fun ChannelStickyBar(
    uiState: ChannelUiState,
    onSortChange: (ChannelSortOrder) -> Unit,
    onModelExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF040404))
    ) {
        ChannelSortBar(
            selectedSort = uiState.currentSort,
            onSortChange = onSortChange,
        )

        if (uiState.hasModelFilters) {
            ChannelModelFilterBar(
                header = uiState.header,
                selectedModel = uiState.selectedModel,
                models = uiState.availableModels,
                isExpanded = uiState.isModelFilterExpanded,
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
        onSortChange = {},
        onModelExpandedChange = {},
        onSelectModel = {},
    )
}
