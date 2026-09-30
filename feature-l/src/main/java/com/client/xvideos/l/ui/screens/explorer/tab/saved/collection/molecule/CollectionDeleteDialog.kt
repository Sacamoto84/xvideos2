package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog

@Composable
fun CollectionDeleteDialog(
    pending: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val dialogBody = remember(pending) {
        buildAnnotatedString {
            append("Удалить «")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending) }
            append("» из коллекции")
        }
    }
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
private fun CollectionDeleteDialogPreview() {
    CollectionDeleteDialog(
        pending = "Favorites",
        onDismiss = {},
        onConfirm = {}
    )
}
