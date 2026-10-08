package com.client.xvideos.screenSettings.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Строка «Порт» веб-сервера с окном смены.
 *
 * Порт хранился в настройках, но сменить его было нечем: при занятом порте
 * сервер не запускался, и поправить это из приложения было нельзя. Пока сервер
 * работает, порт не меняется — он уже занят этим сервером.
 *
 * @param port Текущий порт.
 * @param isRunning Сервер работает.
 * @param onSavePort Сохранение нового, уже проверенного порта.
 */
@Composable
internal fun WebServerPortRow(
    port: Int,
    isRunning: Boolean,
    onSavePort: (Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val onClick: () -> Unit = remember(isRunning) {
        {
            if (isRunning) {
                SnackBar.info("Остановите сервер, чтобы сменить порт")
            } else {
                showDialog = true
            }
        }
    }
    val onDismiss: () -> Unit = remember { { showDialog = false } }
    val onSave: (Int) -> Unit = remember(onSavePort) {
        { newPort ->
            onSavePort(newPort)
            showDialog = false
            SnackBar.success("Порт сохранён")
        }
    }

    SettingsListItem(
        icon = R.drawable.ic_dns_24,
        text = "Порт",
        subtitle = port.toString(),
        onClick = onClick
    )

    if (showDialog) {
        WebServerPortDialog(initialPort = port, onDismiss = onDismiss, onSave = onSave)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun WebServerPortRowPreview() = SettingsPreview {
    SettingsGroup {
        WebServerPortRow(port = 8080, isRunning = false, onSavePort = {})
    }
}
