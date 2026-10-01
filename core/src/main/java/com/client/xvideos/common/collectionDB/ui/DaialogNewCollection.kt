package com.client.xvideos.common.collectionDB.ui

import androidx.compose.runtime.Composable

@Deprecated(
    message = "Используйте корректное написание DialogNewCollection",
    replaceWith = ReplaceWith("DialogNewCollection(visible, onDismiss, onBlockConfirmed)")
)
@Composable
fun DaialogNewCollection(
    visible: Boolean,
    onDismiss: () -> Unit,
    onBlockConfirmed: (String) -> Unit,
) {
    DialogNewCollection(
        visible = visible,
        onDismiss = onDismiss,
        onBlockConfirmed = onBlockConfirmed,
    )
}
