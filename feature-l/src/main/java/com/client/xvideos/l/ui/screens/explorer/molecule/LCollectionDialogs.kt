package com.client.xvideos.l.ui.screens.explorer.molecule

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.collectionDB.ui.DialogNewCollection
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.explorer.model.LCollectionDialogBackAction
import com.client.xvideos.l.ui.screens.explorer.model.resolveLCollectionDialogBackAction

private const val TEXT_ADD_TO_COLLECTION = "Добавить в коллекцию"

@Composable
fun LCollectionDialogs(
    savedL: SavedL,
    modifier: Modifier = Modifier,
) {
    val isAnyDialogOpen = savedL.collection.visibleDialogCreateNew || savedL.collection.visibleDialog
    val onBack = remember(savedL) {
        {
            when (resolveLCollectionDialogBackAction(
                visibleDialogCreateNew = savedL.collection.visibleDialogCreateNew,
                visibleDialog = savedL.collection.visibleDialog
            )) {
                LCollectionDialogBackAction.DISMISS_NEW_COLLECTION -> savedL.collection.visibleDialogCreateNew = false
                LCollectionDialogBackAction.DISMISS_COLLECTION_PICKER -> savedL.collection.visibleDialog = false
                LCollectionDialogBackAction.NONE -> Unit
            }
        }
    }
    BackHandler(enabled = isAnyDialogOpen, onBack = onBack)

    if (savedL.collection.visibleDialogCreateNew) {
        val onDismissNew = remember(savedL) { { savedL.collection.visibleDialogCreateNew = false } }
        val onBlockConfirmed: (String) -> Unit = remember(savedL) {
            { collection ->
                if (collection.isNotEmpty()) {
                    savedL.collection.createCollection(collection)
                    savedL.collection.visibleDialogCreateNew = false
                }
            }
        }
        DialogNewCollection(
            visible = savedL.collection.visibleDialogCreateNew,
            onDismiss = onDismissNew,
            onBlockConfirmed = onBlockConfirmed
        )
    }

    if (savedL.collection.visibleDialog) {
        val haptic = LocalHapticFeedback.current
        val onDismissDialog: () -> Unit = remember(savedL) { { savedL.collection.visibleDialog = false } }
        val onConfirmCreate: () -> Unit = remember(savedL) {
            {
                savedL.collection.visibleDialog = false
                savedL.collection.visibleDialogCreateNew = true
            }
        }
        val title = remember(savedL.collection.collectionItemsPendingAdd.size) {
            val pendingCount = savedL.collection.collectionItemsPendingAdd.size
            if (pendingCount > 1) {
                "$TEXT_ADD_TO_COLLECTION ($pendingCount)"
            } else {
                TEXT_ADD_TO_COLLECTION
            }
        }

        L_DialogCollection(
            title = title,
            collectionList = savedL.collection.collectionList,
            onDismiss = onDismissDialog,
            onConfirmCreate = onConfirmCreate,
            onItemClick = { collectionName ->
                savedL.collection.addPendingToCollection(collectionName)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
            modifier = modifier,
        )
    }
}

@Preview
@Composable
private fun LCollectionDialogsPreview() {
    // SavedL собирается только через DI, поэтому превью показывает выбор коллекции
    // в том виде, в каком его открывает LCollectionDialogs.
    L_DialogCollection(
        title = TEXT_ADD_TO_COLLECTION,
        collectionList = emptyList(),
        onDismiss = {},
        onConfirmCreate = {},
        onItemClick = {}
    )
}
