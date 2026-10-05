package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.net.doh.parseDohUrl
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.screenSettings.components.SettingsPreview

@Composable
internal fun CustomDohUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tempUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    // Сохраняется только проверенный адрес: раньше принимался любой текст с
    // сообщением «сохранён», и опечатка молча меняла резолвер.
    val onConfirmSave: () -> Unit = remember(onSave) {
        {
            parseDohUrl(tempUrl)
                .onSuccess(onSave)
                .onFailure { SnackBar.error(it.message ?: "Адрес DoH не подходит") }
        }
    }
    val onUrlChange: (String) -> Unit = remember { { newUrl -> tempUrl = newUrl } }
    val placeholderContent: @Composable () -> Unit = remember {
        { Text("https://dns.example.com/dns-query") }
    }
    val dialogContent: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit =
        remember(tempUrl, onUrlChange, placeholderContent) {
            {
                Text(
                    "Введите HTTPS URL эндпоинта DoH резолвера (поддерживаются серверы с JSON API, RFC 8427):",
                    style = Theme.L.Type.caption,
                    color = Theme.L.grey1
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = onUrlChange,
                    placeholder = placeholderContent,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

    LavenderDialog(
        title = "Пользовательский DoH URL",
        onDismiss = onDismiss,
        confirmText = "Сохранить",
        onConfirm = onConfirmSave,
        content = dialogContent
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun CustomDohUrlDialogPreview() = SettingsPreview {
    CustomDohUrlDialog(
        initialUrl = "https://dns.google/dns-query",
        onDismiss = {},
        onSave = {}
    )
}
