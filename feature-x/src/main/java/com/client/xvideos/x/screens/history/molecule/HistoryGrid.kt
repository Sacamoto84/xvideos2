package com.client.xvideos.x.screens.history.molecule

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.XHistoryItem

/**
 * Сетка карточек роликов в истории.
 */
@Composable
fun HistoryGrid(
    history: List<XHistoryItem>,
    gridState: LazyGridState,
    selectedIds: List<Long>,
    isSelectionMode: Boolean,
    isFavorite: (ItemsX) -> Boolean,
    localUrlOf: (ItemsX) -> String?,
    posterUrlOf: (ItemsX) -> String,
    actions: HistoryRowActions,
    onToggleSelect: (Long) -> Unit,
    onStartSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = modifier,
    ) {
        items(
            items = history,
            key = { historyItem -> historyItem.item.id },
            contentType = { "history_row" }
        ) { historyItem ->
            val isSelected = historyItem.item.id in selectedIds
            val localUrl = remember(historyItem.item, localUrlOf) { localUrlOf(historyItem.item) }
            val posterUrl = remember(historyItem.item, posterUrlOf) { posterUrlOf(historyItem.item) }
            val favorite = isFavorite(historyItem.item)

            HistoryRow(
                historyItem = historyItem,
                isFavorite = favorite,
                localUrl = localUrl,
                posterUrl = posterUrl,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                onToggleSelect = onToggleSelect,
                onStartSelection = onStartSelection,
                actions = actions,
            )
        }
    }
}

@Preview
@Composable
private fun HistoryGridPreview() {
    HistoryGrid(
        history = listOf(
            XHistoryItem(item = ItemsX(id = 1L, title = "Sample video", duration = "12:34"))
        ),
        gridState = rememberLazyGridState(),
        selectedIds = emptyList(),
        isSelectionMode = false,
        isFavorite = { false },
        localUrlOf = { null },
        posterUrlOf = { "" },
        actions = HistoryRowActions(
            onToggleFavorite = {},
            onDelete = {},
            onDownload = {},
            onPlayLocal = { _, _ -> },
            onOpenVideo = {},
            onSaveToGallery = {},
        ),
        onToggleSelect = {},
        onStartSelection = {},
    )
}
