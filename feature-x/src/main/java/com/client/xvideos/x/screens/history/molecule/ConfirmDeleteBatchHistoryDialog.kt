package com.client.xvideos.x.screens.history.molecule

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.LavenderDialog

/**
 * Диалог подтверждения удаления выбранной группы роликов из истории.
 */
@Composable
fun ConfirmDeleteBatchHistoryDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val messageText = remember(count) {
        "Будет удалено $count видео из истории просмотров."
    }
    LavenderDialog(
        title = "Удалить выбранные?",
        content = {
            Text(
                text = messageText,
                color = Color.White,
                fontSize = 14.sp,
            )
        },
        onDismiss = onDismiss,
        confirmText = "Удалить",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Preview
@Composable
private fun ConfirmDeleteBatchHistoryDialogPreview() {
    ConfirmDeleteBatchHistoryDialog(
        count = 5,
        onConfirm = {},
        onDismiss = {}
    )
}
