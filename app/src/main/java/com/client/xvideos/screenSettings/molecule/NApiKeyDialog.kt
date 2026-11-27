package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.ui.excludedFromAutofill
import com.client.xvideos.n.net.NAuthState

@Composable
internal fun NApiKeyDialog(hasApiKey: Boolean, onDismiss: () -> Unit) {
    var key by remember { mutableStateOf("") }
    val trimmedKey = key.trim()
    val validKey = trimmedKey.isNotEmpty() && trimmedKey.all { it in '!'..'~' }
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasApiKey) "Изменить API-ключ nhentai" else "API-ключ nhentai") },
        text = {
            Column {
                Text("Создайте ключ в аккаунте, где сохранены ваши альбомы.")
                TextButton(onClick = { uriHandler.openUri("https://nhentai.net/user/settings#apikeys") }) {
                    Text("Открыть настройки сайта")
                }
                OutlinedTextField(
                    value = key,
                    modifier = Modifier.excludedFromAutofill(),
                    onValueChange = { key = it },
                    label = { Text(if (hasApiKey) "Новый API-ключ" else "API-ключ") },
                    isError = trimmedKey.isNotEmpty() && !validKey,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = validKey, onClick = {
                if (!NAuthState.setApiKey(trimmedKey)) {
                    SnackBar.info("Ключ действует до перезапуска. Защищённое хранилище недоступно.")
                }
                onDismiss()
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Preview
@Composable
private fun NApiKeyDialogPreview() {
    NApiKeyDialog(hasApiKey = false, onDismiss = {})
}
