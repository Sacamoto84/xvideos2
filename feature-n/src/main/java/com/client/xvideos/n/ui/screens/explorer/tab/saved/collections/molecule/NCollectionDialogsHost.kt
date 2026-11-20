package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.model.NCollectionDialogData

/**
 * Хост модальных диалогов управления коллекциями манги (действие, переименование, удаление).
 */
@Composable
fun NCollectionDialogsHost(
    dialogData: NCollectionDialogData,
    collections: List<CollectionEntity<NPicture>>,
    onDismissAction: () -> Unit,
    onDismissRename: () -> Unit,
    onDismissDelete: () -> Unit,
    onRenameAction: (String) -> Unit,
    onDeleteAction: (String) -> Unit,
    onConfirmRename: (String, String) -> Unit,
    onConfirmDelete: (String) -> Unit,
    coverUrlProvider: (String, List<NPicture>) -> String? = { _, items -> items.firstOrNull()?.thumbnailUrl }
) {
    dialogData.itemPendingAction?.let { pending ->
        val entity = collections.firstOrNull { it.collection.equals(pending, ignoreCase = true) }
        val cover = entity?.let { coverUrlProvider(it.collection, it.items) }
        val handleRename = remember(pending, onRenameAction) { { onRenameAction(pending) } }
        val handleDelete = remember(pending, onDeleteAction) { { onDeleteAction(pending) } }

        NCollectionActionDialog(
            pending = pending,
            coverUrl = cover,
            onDismiss = onDismissAction,
            onRename = handleRename,
            onDelete = handleDelete
        )
    }

    dialogData.itemPendingRename?.let { pending ->
        val handleConfirmRename = remember(pending, onConfirmRename) {
            { targetName: String -> onConfirmRename(pending, targetName) }
        }
        NCollectionRenameDialog(
            initialValue = dialogData.renameValue,
            onDismiss = onDismissRename,
            onConfirm = handleConfirmRename
        )
    }

    dialogData.itemPendingDelete?.let { pending ->
        val handleConfirmDelete = remember(pending, onConfirmDelete) {
            { onConfirmDelete(pending) }
        }
        NCollectionDeleteDialog(
            pending = pending,
            onDismiss = onDismissDelete,
            onConfirm = handleConfirmDelete
        )
    }
}

@Preview
@Composable
private fun NCollectionDialogsHostPreview() {
    NCollectionDialogsHost(
        dialogData = NCollectionDialogData(itemPendingAction = "Избранные страницы"),
        collections = emptyList(),
        onDismissAction = {},
        onDismissRename = {},
        onDismissDelete = {},
        onRenameAction = {},
        onDeleteAction = {},
        onConfirmRename = { _, _ -> },
        onConfirmDelete = {}
    )
}
