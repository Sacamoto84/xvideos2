package com.client.xvideos.r.ui.root.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.collectionDB.ui.DaialogNewCollection
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.ui.block.DialogBlock
import com.client.xvideos.r.ui.root.DialogCollection

@Composable
fun RedRootDialogs(
    savedRed: () -> SavedRed,
    block: () -> BlockRed,
) {
    val haptic = LocalHapticFeedback.current

    if (savedRed().collections.visibleDialog) {
        DialogCollection(
            visible = savedRed().collections.visibleDialog,
            onDismiss = { savedRed().collections.visibleDialog = false },
            onClickNewCollection = {
                savedRed().collections.visibleDialogCreateNew = true
            },
            onSelectCollection = { collection ->
                savedRed().collections.collectionItemGifInfo?.let { item ->
                    savedRed().collections.addCollection(item, collection)
                }
                savedRed().collections.visibleDialog = false
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
            savedRed = savedRed
        )
    }

    if (savedRed().collections.visibleDialogCreateNew) {
        DaialogNewCollection(
            visible = savedRed().collections.visibleDialogCreateNew,
            onDismiss = { savedRed().collections.visibleDialogCreateNew = false },
            onBlockConfirmed = { collection ->
                if (collection.isNotEmpty()) {
                    savedRed().collections.createCollection(collection)
                    savedRed().collections.visibleDialogCreateNew = false
                }
            }
        )
    }

    if (block().blockVisibleDialog) {
        DialogBlock(
            visible = block().blockVisibleDialog,
            onDismiss = { block().blockVisibleDialog = false },
            onBlockConfirmed = {
                block().blockItem?.let { item ->
                    block().blockItem(item)
                    block().blockItem = null
                }
            }
        )
    }
}

@Preview
@Composable
private fun RedRootDialogsPreview() {
    // Lightweight preview with no active dialogs
}
