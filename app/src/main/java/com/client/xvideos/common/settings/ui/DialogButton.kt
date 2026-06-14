package com.client.xvideos.common.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog

@Composable
fun DialogButton(
    visible: Boolean,
    title: String,
    body: String,
    buttonText: String,
    onDismiss: () -> Unit,
    onBlockConfirmed: () -> Unit,
    composable: @Composable () -> Unit = {}
) {
    if (visible) {
        LavenderDialog(
            title = title,
            onDismiss = onDismiss,
            body = if (body.isNotEmpty()) AnnotatedString(body) else null,
            content = { composable() },
            confirmText = buttonText,
            onConfirm = {
                onBlockConfirmed()
                onDismiss()
            },
        )
    }
}

@Preview
@Composable
fun DialogButtonPreview() {
    DialogButton(
        visible = true,
        title = "Dialog Title",
        body = "This is the body of the dialog.",
        buttonText = "Confirm",
        onDismiss = {},
        onBlockConfirmed = {}
    )
}
