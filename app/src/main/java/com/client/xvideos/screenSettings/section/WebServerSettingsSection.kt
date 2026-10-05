package com.client.xvideos.screenSettings.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.R
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.webserver.NetworkIpHelper
import com.client.xvideos.common.webserver.QrCodeGenerator
import com.client.xvideos.common.webserver.WebServerService
import com.client.xvideos.common.webserver.WebServerState
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.screenSettings.components.SettingsSwitchRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.client.xvideos.screenSettings.molecule.WebServerConnectionCard



@Suppress("DEPRECATION")
@Composable
internal fun WebServerSettingsSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val isRunning by WebServerState.isRunning.collectAsStateWithLifecycle()
    val serverUrl by WebServerState.serverUrl.collectAsStateWithLifecycle()
    val networkName by WebServerState.networkName.collectAsStateWithLifecycle()
    val lastError by WebServerState.lastError.collectAsStateWithLifecycle()

    val keepAwake by Settings.web_server_keep_awake.field.collectAsStateWithLifecycle()
    val port by Settings.web_server_port.field.collectAsStateWithLifecycle()

    val qrBitmap by produceState<ImageBitmap?>(initialValue = null, serverUrl) {
        value = serverUrl?.let { url ->
            withContext(Dispatchers.Default) {
                QrCodeGenerator.generateImageBitmap(
                    content = url,
                    sizePx = 480,
                    darkColor = android.graphics.Color.BLACK,
                    lightColor = android.graphics.Color.WHITE,
                    margin = 1
                )
            }
        }
    }

    val onToggleServer: (Boolean) -> Unit = remember(context, port) {
        { enable ->
            if (enable) {
                val ip = NetworkIpHelper.getLocalIpAddress(context)
                if (ip == null) {
                    SnackBar.error("Подключитесь к Wi-Fi или включите точку доступа")
                } else {
                    WebServerService.start(context, port)
                    SnackBar.info("Запуск веб-сервера...")
                }
            } else {
                WebServerService.stop(context)
                SnackBar.info("Веб-сервер остановлен")
            }
        }
    }
    val onKeepAwakeChange: (Boolean) -> Unit = remember {
        { enabled ->
            Settings.web_server_keep_awake.setValue(enabled)
        }
    }

    val serverSubtitle = remember(isRunning, serverUrl) {
        if (isRunning) "Работает: $serverUrl" else "Сервер выключен"
    }

    Column(modifier = if (modifier == Modifier) Modifier.fillMaxWidth() else modifier.fillMaxWidth()) {
        SettingsSectionTitle("Веб-сервер Wi-Fi")

        SettingsGroup {
            SettingsSwitchRow(
                icon = R.drawable.hard_drive_2_24,
                text = "Трансляция на ПК",
                subtitle = serverSubtitle,
                value = isRunning,
                onValueChange = onToggleServer
            )

            SettingsDivider()

            SettingsSwitchRow(
                icon = R.drawable.memory_24,
                text = "Не усыплять Wi-Fi и процессор",
                subtitle = "Стабильный стриминг при заблокированном экране",
                value = keepAwake,
                onValueChange = onKeepAwakeChange
            )
        }

        if (lastError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ошибка: $lastError",
                color = Color(0xFFFF5252),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        val currentServerUrl = serverUrl
        if (isRunning && currentServerUrl != null) {
            Spacer(Modifier.height(16.dp))
            SettingsSectionTitle("Подключение")
            WebServerConnectionCard(
                serverUrl = currentServerUrl,
                networkName = networkName,
                qrBitmap = qrBitmap
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun WebServerSettingsSectionPreview() = com.client.xvideos.screenSettings.components.SettingsPreview {
    WebServerSettingsSection()
}

