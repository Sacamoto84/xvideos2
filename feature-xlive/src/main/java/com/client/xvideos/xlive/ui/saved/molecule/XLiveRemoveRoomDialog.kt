package com.client.xvideos.xlive.ui.saved.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog

/**
 * Подтверждение: убрать комнату [username] из «Моих» или «Истории» — тот же
 * диалог, что у отписки в X.
 *
 * @param listName список в родительном падеже: «Моих», «истории».
 */
@Composable
fun XLiveRemoveRoomDialog(
    username: String,
    listName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val body = remember(username, listName) {
        buildAnnotatedString {
            append("Убрать «")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(username) }
            append("» из $listName?")
        }
    }
    LavenderDialog(
        title = "Убрать комнату?",
        onDismiss = onDismiss,
        body = body,
        confirmText = "Убрать",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Preview
@Composable
private fun XLiveRemoveRoomDialogPreview() {
    XLiveRemoveRoomDialog(username = "room_a", listName = "«Моих»", onDismiss = {}, onConfirm = {})
}
