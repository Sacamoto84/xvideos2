package com.client.xvideos.x.screens.history.molecule

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.LavenderDialog

/**
 * Диалог подтверждения полной очистки истории просмотров.
 */
@Composable
fun ConfirmClearAllHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LavenderDialog(
        title = "Очистить историю?",
        content = {
            Text(
                text = "Все записи истории просмотров будут безвозвратно удалены.",
                color = Color.White,
                fontSize = 14.sp,
            )
        },
        onDismiss = onDismiss,
        confirmText = "Очистить",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Preview
@Composable
private fun ConfirmClearAllHistoryDialogPreview() {
    ConfirmClearAllHistoryDialog(
        onConfirm = {},
        onDismiss = {}
    )
}
