package com.client.xvideos.x.screens.history.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.x.model.ItemsX

/**
 * Сборный контейнер диалогов подтверждения для экрана истории.
 */
@Composable
fun HistoryDialogs(
    pendingDelete: ItemsX?,
    posterUrlOf: (ItemsX) -> String,
    onConfirmDelete: (ItemsX) -> Unit,
    onDismissDelete: () -> Unit,
    showClearAllConfirm: Boolean,
    onConfirmClearAll: () -> Unit,
    onDismissClearAll: () -> Unit,
    showBatchDeleteConfirm: Boolean,
    batchDeleteCount: Int,
    onConfirmBatchDelete: () -> Unit,
    onDismissBatchDelete: () -> Unit,
) {
    pendingDelete?.let { item ->
        ConfirmDeleteHistoryDialog(
            item = item,
            posterUrl = posterUrlOf(item),
            onConfirm = { onConfirmDelete(item) },
            onDismiss = onDismissDelete,
        )
    }

    if (showClearAllConfirm) {
        ConfirmClearAllHistoryDialog(
            onConfirm = onConfirmClearAll,
            onDismiss = onDismissClearAll,
        )
    }

    if (showBatchDeleteConfirm) {
        ConfirmDeleteBatchHistoryDialog(
            count = batchDeleteCount,
            onConfirm = onConfirmBatchDelete,
            onDismiss = onDismissBatchDelete,
        )
    }
}

@Preview
@Composable
private fun HistoryDialogsPreview() {
    HistoryDialogs(
        pendingDelete = null,
        posterUrlOf = { "" },
        onConfirmDelete = {},
        onDismissDelete = {},
        showClearAllConfirm = false,
        onConfirmClearAll = {},
        onDismissClearAll = {},
        showBatchDeleteConfirm = false,
        batchDeleteCount = 0,
        onConfirmBatchDelete = {},
        onDismissBatchDelete = {},
    )
}
