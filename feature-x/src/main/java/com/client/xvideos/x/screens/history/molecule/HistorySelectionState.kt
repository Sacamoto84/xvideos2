package com.client.xvideos.x.screens.history.molecule

import androidx.compose.runtime.Immutable

@Immutable
data class HistorySelectionState(
    val isSelectionMode: Boolean = false,
    val isSelected: Boolean = false,
    val onToggleSelect: () -> Unit = {},
    val onStartSelection: () -> Unit = {},
)
