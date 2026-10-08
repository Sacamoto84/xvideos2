package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.webserver.WEB_SERVER_MAX_PORT
import com.client.xvideos.common.webserver.WEB_SERVER_MIN_PORT
import com.client.xvideos.common.webserver.parseWebServerPort
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Окно смены порта веб-сервера. Отдаёт наружу только проверенный порт.
 *
 * @param initialPort Текущий порт.
 * @param onDismiss Закрытие без сохранения.
 * @param onSave Сохранение проверенного порта.
 */
@Composable
internal fun WebServerPortDialog(
    initialPort: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var portText by remember(initialPort) { mutableStateOf(initialPort.toString()) }
    val onConfirmSave: () -> Unit = remember(onSave) {
        {
            parseWebServerPort(portText)
                .onSuccess(onSave)
                .onFailure { SnackBar.error(it.message ?: "Порт не подходит") }
        }
    }
    val onPortChange: (String) -> Unit = remember { { newText -> portText = newText } }
    val dialogContent: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit =
        remember(portText, onPortChange) {
            {
                Text(
                    "Число от $WEB_SERVER_MIN_PORT до $WEB_SERVER_MAX_PORT. Смените порт, если прежний занят другим приложением.",
                    style = Theme.L.Type.caption,
                    color = Theme.L.grey1
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = portText,
                    onValueChange = onPortChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

    LavenderDialog(
        title = "Порт веб-сервера",
        onDismiss = onDismiss,
        confirmText = "Сохранить",
        onConfirm = onConfirmSave,
        content = dialogContent
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun WebServerPortDialogPreview() = SettingsPreview {
    WebServerPortDialog(
        initialPort = 8080,
        onDismiss = {},
        onSave = {}
    )
}
