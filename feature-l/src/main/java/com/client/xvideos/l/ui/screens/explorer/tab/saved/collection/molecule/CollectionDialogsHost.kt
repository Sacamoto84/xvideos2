package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.featured.saved.LCollectionEntity

@Composable
fun CollectionDialogsHost(
    dialogData: CollectionDialogData,
    collectionList: List<LCollectionEntity>,
    onDismissAction: () -> Unit,
    onDismissRename: () -> Unit,
    onDismissDelete: () -> Unit,
    onRenameAction: (String) -> Unit,
    onShareAction: (String) -> Unit,
    onDeleteAction: (String) -> Unit,
    onConfirmRename: (String, String) -> Unit,
    onConfirmDelete: (String) -> Unit,
) {
    dialogData.itemPendingAction?.let { pending ->
        val cover = collectionList
            .firstOrNull { it.collection == pending }?.previewUrl
        val handleRename = remember(pending, onRenameAction) { { onRenameAction(pending) } }
        val handleShare = remember(pending, onShareAction) { { onShareAction(pending) } }
        val handleDelete = remember(pending, onDeleteAction) { { onDeleteAction(pending) } }

        CollectionActionDialog(
            pending = pending,
            coverUrl = cover,
            onDismiss = onDismissAction,
            onRename = handleRename,
            onShare = handleShare,
            onDelete = handleDelete
        )
    }

    dialogData.itemPendingRename?.let { pending ->
        val handleConfirmRename = remember(pending, onConfirmRename) {
            { targetName: String -> onConfirmRename(pending, targetName) }
        }
        CollectionRenameDialog(
            initialValue = dialogData.renameValue,
            onDismiss = onDismissRename,
            onConfirm = handleConfirmRename
        )
    }

    dialogData.itemPendingDelete?.let { pending ->
        val handleConfirmDelete = remember(pending, onConfirmDelete) {
            { onConfirmDelete(pending) }
        }
        CollectionDeleteDialog(
            pending = pending,
            onDismiss = onDismissDelete,
            onConfirm = handleConfirmDelete
        )
    }
}

@Preview
@Composable
private fun CollectionDialogsHostPreview() {
    CollectionDialogsHost(
        dialogData = CollectionDialogData(),
        collectionList = emptyList(),
        onDismissAction = {},
        onDismissRename = {},
        onDismissDelete = {},
        onRenameAction = {},
        onShareAction = {},
        onDeleteAction = {},
        onConfirmRename = { _, _ -> },
        onConfirmDelete = {}
    )
}
