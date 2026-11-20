package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.collectionDeleteBody

/**
 * Диалог подтверждения удаления коллекции манги.
 */
@Composable
fun NCollectionDeleteDialog(
    pending: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val dialogBody = remember(pending) { collectionDeleteBody(pending) }
    LavenderDialog(
        title = "Удалить коллекцию?",
        onDismiss = onDismiss,
        body = dialogBody,
        confirmText = "Удалить",
        onConfirm = onConfirm,
        destructive = true
    )
}

@Preview
@Composable
private fun NCollectionDeleteDialogPreview() {
    NCollectionDeleteDialog(
        pending = "Избранное",
        onDismiss = {},
        onConfirm = {}
    )
}
