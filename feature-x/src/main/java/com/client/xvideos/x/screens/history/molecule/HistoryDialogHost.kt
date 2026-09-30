package com.client.xvideos.x.screens.history.molecule
import com.client.xvideos.x.screens.history.model.HistoryDialogData

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.XHistoryItem

/**
 * Хост всех диалогов экрана истории.
 */
@Composable
fun HistoryDialogHost(
    dialogData: HistoryDialogData,
    posterUrlOf: (ItemsX) -> String,
    onDelete: (ItemsX) -> Unit,
    onClearAll: () -> Unit,
    onDeleteBatch: (Collection<ItemsX>) -> Unit,
    history: List<XHistoryItem>,
    onDismissDelete: () -> Unit,
    onDismissClearAll: () -> Unit,
    onDismissBatchDelete: () -> Unit,
) {
    val handleConfirmDelete = remember(onDelete, onDismissDelete) {
        { item: ItemsX ->
            onDelete(item)
            onDismissDelete()
        }
    }
    val handleConfirmClearAll = remember(onClearAll, onDismissClearAll) {
        {
            onClearAll()
            onDismissClearAll()
        }
    }
    val handleConfirmBatchDelete = remember(history, dialogData.selectedIds, onDeleteBatch) {
        {
            val items = history.map { it.item }.filter { it.id in dialogData.selectedIds }
            onDeleteBatch(items)
        }
    }

    HistoryDialogs(
        pendingDelete = dialogData.pendingDelete,
        posterUrlOf = posterUrlOf,
        onConfirmDelete = handleConfirmDelete,
        onDismissDelete = onDismissDelete,
        showClearAllConfirm = dialogData.showClearAllConfirm,
        onConfirmClearAll = handleConfirmClearAll,
        onDismissClearAll = onDismissClearAll,
        showBatchDeleteConfirm = dialogData.showBatchDeleteConfirm,
        batchDeleteCount = dialogData.selectedIds.size,
        onConfirmBatchDelete = handleConfirmBatchDelete,
        onDismissBatchDelete = onDismissBatchDelete,
    )
}

@Preview
@Composable
private fun HistoryDialogHostPreview() {
    HistoryDialogHost(
        dialogData = HistoryDialogData(),
        posterUrlOf = { "" },
        onDelete = {},
        onClearAll = {},
        onDeleteBatch = {},
        history = emptyList(),
        onDismissDelete = {},
        onDismissClearAll = {},
        onDismissBatchDelete = {},
    )
}
