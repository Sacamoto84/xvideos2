package com.client.xvideos.screenSettings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier,
    destructive: Boolean = true,
    composable: (@Composable () -> Unit)? = null
) {
    if (visible) {
        val bodyAnnotated = remember(body) {
            if (body.isNotBlank()) AnnotatedString(body) else null
        }
        val handleConfirm = remember(onBlockConfirmed, onDismiss) {
            {
                onBlockConfirmed()
                onDismiss()
            }
        }
        val dialogContent: (@Composable ColumnScope.() -> Unit)? = remember(composable) {
            composable?.let { content ->
                { content() }
            }
        }

        LavenderDialog(
            title = title,
            onDismiss = onDismiss,
            body = bodyAnnotated,
            content = dialogContent,
            confirmText = buttonText,
            onConfirm = handleConfirm,
            destructive = destructive,
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
