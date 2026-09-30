package com.client.xvideos.x.screens.history.molecule
import com.client.xvideos.x.screens.history.model.HistorySelectionState
import com.client.xvideos.x.screens.history.model.HistoryRowActions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.XHistoryItem
import com.client.xvideos.x.screens.history.atom.DurationOverlay
import com.client.xvideos.x.screens.history.atom.HistoryWatchedBadge
import com.client.xvideos.x.screens.history.atom.SelectionCheckBadge

private const val CARD_ASPECT_RATIO = 352f / 198f

/**
 * Карточка одного элемента в сетке истории просмотров.
 */
@Composable
fun HistoryRow(
    historyItem: XHistoryItem,
    isFavorite: Boolean,
    localUrl: String?,
    posterUrl: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: (Long) -> Unit,
    onStartSelection: (Long) -> Unit,
    actions: HistoryRowActions,
    modifier: Modifier = Modifier,
) {
    val item = historyItem.item

    val selectionState = remember(isSelectionMode, isSelected, item.id, onToggleSelect, onStartSelection) {
        HistorySelectionState(
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onToggleSelect = { onToggleSelect(item.id) },
            onStartSelection = { onStartSelection(item.id) },
        )
    }
    val handleToggleFavorite = remember(item, actions.onToggleFavorite) { { actions.onToggleFavorite(item) } }
    val handleDelete = remember(item, actions.onDelete) { { actions.onDelete(item) } }
    val handleDownload = remember(item, actions.onDownload) { { actions.onDownload(item) } }
    val handlePlayLocal = remember(item, actions.onPlayLocal) { { url: String -> actions.onPlayLocal(url, item) } }
    val handleOpenVideo = remember(item, actions.onOpenVideo) { { actions.onOpenVideo(item) } }
    val handleSaveToGallery = remember(item, actions.onSaveToGallery) { { actions.onSaveToGallery(item) } }

    val cardModifier = modifier
        .fillMaxWidth()
        .padding(1.dp)
        .aspectRatio(CARD_ASPECT_RATIO)
        .background(Color.DarkGray)
        .then(
            if (selectionState.isSelected) {
                Modifier.border(2.dp, Color(0xFFE91E63))
            } else {
                Modifier
            }
        )

    Box(
        modifier = cardModifier
    ) {
        HistoryCardMedia(
            item = item,
            localUrl = localUrl,
            posterUrl = posterUrl,
            isSelectionMode = selectionState.isSelectionMode,
            onToggleSelect = selectionState.onToggleSelect,
            onStartSelection = selectionState.onStartSelection,
            onPlayLocal = handlePlayLocal,
            onOpenVideo = handleOpenVideo,
        )

        if (selectionState.isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (selectionState.isSelected) Color(0x55E91E63) else Color.Transparent)
                    .clickable(onClick = selectionState.onToggleSelect)
            )
            SelectionCheckBadge(
                isSelected = selectionState.isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        } else {
            Row(Modifier.align(Alignment.TopEnd)) {
                HistoryActionsMenu(
                    isFavorite = isFavorite,
                    onToggleFavorite = handleToggleFavorite,
                    onDelete = handleDelete,
                    onDownload = handleDownload,
                    onSaveToGallery = handleSaveToGallery,
                )
            }
        }

        if (historyItem.isCompleted) {
            HistoryWatchedBadge(Modifier.align(Alignment.BottomStart))
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 4.dp)
        ) {
            DurationOverlay(item.duration)
        }

        if (historyItem.lastPositionMs > 0L) {
            val progressFraction = historyItem.progressFraction
            val progressProvider: () -> Float = remember(progressFraction) { { progressFraction } }
            LinearProgressIndicator(
                progress = progressProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = Color(0xFFFF3333),
                trackColor = Color(0x66000000),
            )
        }
    }
}

@Preview
@Composable
private fun HistoryRowPreview() {
    HistoryRow(
        historyItem = XHistoryItem(item = ItemsX(id = 1L, title = "Sample video", duration = "12:34")),
        isFavorite = false,
        localUrl = null,
        posterUrl = "",
        isSelectionMode = false,
        isSelected = false,
        onToggleSelect = {},
        onStartSelection = {},
        actions = HistoryRowActions(
            onToggleFavorite = {},
            onDelete = {},
            onDownload = {},
            onPlayLocal = { _, _ -> },
            onOpenVideo = {},
            onSaveToGallery = {},
        )
    )
}
