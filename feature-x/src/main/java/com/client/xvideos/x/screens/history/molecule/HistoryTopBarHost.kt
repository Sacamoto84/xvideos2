package com.client.xvideos.x.screens.history.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.x.model.XHistoryItem

/**
 * Хост верхней панели истории, управляющий логикой выбора всех элементов и переключения режимов.
 */
@Composable
fun HistoryTopBarHost(
    isSelectionMode: Boolean,
    selectedIds: SnapshotStateList<Long>,
    history: List<XHistoryItem>,
    onSetSelectionMode: (Boolean) -> Unit,
    onDeleteBatch: () -> Unit,
    onClearAll: () -> Unit,
) {
    val onEnterSelectionMode = remember(onSetSelectionMode) { { onSetSelectionMode(true) } }
    val onExitSelectionMode = remember(onSetSelectionMode, selectedIds) {
        {
            onSetSelectionMode(false)
            selectedIds.clear()
        }
    }
    val onToggleSelectAll: () -> Unit = remember(history, selectedIds) {
        {
            if (selectedIds.size == history.size) {
                selectedIds.clear()
            } else {
                selectedIds.clear()
                val ignored = selectedIds.addAll(history.map { it.item.id })
            }
        }
    }
    val allSelected = history.isNotEmpty() && selectedIds.size == history.size
    HistoryTopBar(
        isSelectionMode = isSelectionMode,
        selectedCount = selectedIds.size,
        allSelected = allSelected,
        hasItems = history.isNotEmpty(),
        onEnterSelectionMode = onEnterSelectionMode,
        onExitSelectionMode = onExitSelectionMode,
        onToggleSelectAll = onToggleSelectAll,
        onDeleteBatch = onDeleteBatch,
        onClearAll = onClearAll,
    )
}

@Preview
@Composable
private fun HistoryTopBarHostPreview() {
    val ids = remember { androidx.compose.runtime.mutableStateListOf<Long>() }
    HistoryTopBarHost(
        isSelectionMode = false,
        selectedIds = ids,
        history = emptyList(),
        onSetSelectionMode = {},
        onDeleteBatch = {},
        onClearAll = {},
    )
}
