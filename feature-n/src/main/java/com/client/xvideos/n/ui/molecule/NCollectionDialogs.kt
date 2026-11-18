package com.client.xvideos.n.ui.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.collectionDB.ui.DialogNewCollection
import com.client.xvideos.n.model.NPicture

/**
 * Молекула управления диалогами выбора и создания коллекции для раздела N.
 */
@Composable
fun NCollectionDialogs(
    isSelectOpen: Boolean,
    isCreateOpen: Boolean,
    collections: List<CollectionEntity<NPicture>>,
    onDismiss: () -> Unit,
    onOpenCreate: () -> Unit,
    onCreateCollection: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
    coverUrlProvider: (String, List<NPicture>) -> String? = { _, items -> items.firstOrNull()?.thumbnailUrl }
) {
    if (isCreateOpen) {
        DialogNewCollection(
            visible = isCreateOpen,
            onDismiss = onDismiss,
            onBlockConfirmed = onCreateCollection
        )
    }

    if (isSelectOpen) {
        NDialogSelectCollection(
            collections = collections,
            onDismiss = onDismiss,
            onConfirmCreate = onOpenCreate,
            onSelectCollection = onSelectCollection,
            modifier = modifier,
            coverUrlProvider = coverUrlProvider
        )
    }
}

@Preview
@Composable
private fun NCollectionDialogsPreview() {
    NCollectionDialogs(
        isSelectOpen = false,
        isCreateOpen = false,
        collections = emptyList(),
        onDismiss = {},
        onOpenCreate = {},
        onCreateCollection = {},
        onSelectCollection = {},
        coverUrlProvider = { _, _ -> null }
    )
}
