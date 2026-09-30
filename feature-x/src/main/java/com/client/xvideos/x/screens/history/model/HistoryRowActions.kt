package com.client.xvideos.x.screens.history.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.x.model.ItemsX

@Immutable
data class HistoryRowActions(
    val onToggleFavorite: (ItemsX) -> Unit,
    val onDelete: (ItemsX) -> Unit,
    val onDownload: (ItemsX) -> Unit,
    val onPlayLocal: (String, ItemsX) -> Unit,
    val onOpenVideo: (ItemsX) -> Unit,
    val onSaveToGallery: (ItemsX) -> Unit = {},
)
