package com.client.xvideos.x.screens.history.molecule

import androidx.compose.runtime.Immutable
import com.client.xvideos.x.model.ItemsX

@Immutable
data class HistoryDialogData(
    val pendingDelete: ItemsX? = null,
    val showClearAllConfirm: Boolean = false,
    val showBatchDeleteConfirm: Boolean = false,
    val selectedIds: List<Long> = emptyList(),
)
