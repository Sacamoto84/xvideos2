package com.client.xvideos.r.ui.block

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.ui.theme.XvideosTheme

private val BLOCK_DIALOG_BODY = AnnotatedString("Вы уверены, что хотите заблокировать этот GIFs?")
private const val DIALOG_TITLE = "Подтвердите блокировку"
private const val CONFIRM_TEXT = "Блокировать"

@Composable
fun DialogBlock(
    visible: Boolean,
    onDismiss: () -> Unit,
    onBlockConfirmed: () -> Unit,
) {
    if (visible) {
        val handleConfirm = remember(onBlockConfirmed, onDismiss) {
            {
                onBlockConfirmed()
                onDismiss()
            }
        }
        LavenderDialog(
            title = DIALOG_TITLE,
            onDismiss = onDismiss,
            body = BLOCK_DIALOG_BODY,
            confirmText = CONFIRM_TEXT,
            onConfirm = handleConfirm,
            destructive = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DialogBlockPreview() {
    XvideosTheme {
        DialogBlock(
            visible = true,
            onDismiss = {},
            onBlockConfirmed = {}
        )
    }
}
