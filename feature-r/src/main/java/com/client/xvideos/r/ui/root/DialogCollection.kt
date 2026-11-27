package com.client.xvideos.r.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.r.common.saved.SavedRed

@Composable
fun DialogCollection(
    visible: Boolean,
    onDismiss: () -> Unit,
    onClickNewCollection: () -> Unit,
    onSelectCollection: (String) -> Unit = {},
    savedRed: () -> SavedRed
) {
    if (!visible) return

    val onConfirmCreate = remember(onClickNewCollection, onDismiss) {
        {
            onClickNewCollection()
            onDismiss()
        }
    }

    LavenderDialog(
        title = "Добавить в коллекцию",
        onDismiss = onDismiss,
        content = {
            CollectionListContent(
                collectionList = savedRed().collections.collectionList,
                onSelectCollection = onSelectCollection
            )
        },
        confirmText = "Создать",
        onConfirm = onConfirmCreate,
    )
}
