package com.client.xvideos.l.ui.screens.explorer.molecule

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.client.xvideos.common.collectionDB.ui.DialogNewCollection
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.explorer.model.LCollectionDialogBackAction
import com.client.xvideos.l.ui.screens.explorer.model.resolveLCollectionDialogBackAction

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
        L_DialogCollection(
            savedL = savedL,
            modifier = modifier,
        )
    }
}
