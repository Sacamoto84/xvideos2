package com.client.xvideos.xlive.ui.saved.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog

/** Подтверждение: удалить запись эфира вместе с файлом. */
@Composable
fun XLiveRemoveRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    LavenderDialog(
        title = "Удалить запись?",
        onDismiss = onDismiss,
        body = AnnotatedString("Файл записи удалится с телефона."),
        confirmText = "Удалить",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Preview
@Composable
private fun XLiveRemoveRecordDialogPreview() {
    XLiveRemoveRecordDialog(onDismiss = {}, onConfirm = {})
}
