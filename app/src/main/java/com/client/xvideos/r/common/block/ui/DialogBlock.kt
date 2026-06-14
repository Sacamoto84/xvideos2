package com.redgifs.common.block.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun DialogBlock(
    visible: Boolean,
    onDismiss: () -> Unit,
    onBlockConfirmed: () -> Unit,
) {
    if (visible) {
        LavenderDialog(
            title = "Подтвердите блокировку",
            onDismiss = onDismiss,
            body = AnnotatedString("Вы уверены, что хотите заблокировать этот GIFs?"),
            confirmText = "Блокировать",
            onConfirm = {
                onBlockConfirmed()
                onDismiss()
            },
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
