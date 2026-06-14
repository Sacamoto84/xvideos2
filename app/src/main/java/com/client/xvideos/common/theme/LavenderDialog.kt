package com.client.xvideos.common.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Канонический лавандовый диалог (стиль A). Все настройки — из [Theme.L.DialogLavande].
 * Кнопки: filled главная + text отмена; destructive=true → красная заливка.
 */
@Composable
fun LavenderDialog(
    title: String,
    onDismiss: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    body: AnnotatedString? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissText: String = "Отмена",
) {
    val d = Theme.L.DialogLavande
    val centered = icon != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(d.cornerRadius), color = d.content) {
            Column(Modifier.padding(24.dp)) {

                if (icon != null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { icon() }
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    text = title,
                    style = Theme.L.Type.dialogTitle.copy(color = Color.Black, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                )

                if (body != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = body,
                        style = Theme.L.Type.dialogBody.copy(color = d.bodyColor),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    )
                }

                if (content != null) {
                    Spacer(Modifier.height(12.dp))
                    content()
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissText, style = d.button.copy(color = d.dismissTextColor))
                    }
                    if (confirmText != null) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onConfirm,
                            enabled = confirmEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (destructive) d.buttonBackgroundDestructive else d.buttonBackground,
                                contentColor = d.buttonTextColor,
                            ),
                            shape = RoundedCornerShape(d.buttonBorderRadius),
                        ) {
                            Text(confirmText, color = d.buttonTextColor)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LavenderDialogPreview() {
    LavenderDialog(
        title = "Удалить Альбом?",
        onDismiss = {},
        body = AnnotatedString("Альбом будет удалён из сохранённых."),
        confirmText = "Удалить",
        onConfirm = {},
        destructive = true,
    )
}
