package com.client.xvideos.screenSettings.section

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsCardColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsRowTextSecondary
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.screenSettings.components.SettingsSwitchRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
@Composable
internal fun WebServerSettingsSection() {
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
        {
            Settings.web_server_keep_awake.setValue(it)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionTitle("Веб-сервер Wi-Fi")

        SettingsGroup {
            SettingsSwitchRow(
                icon = R.drawable.hard_drive_2_24,
                text = "Трансляция на ПК",
                subtitle = if (isRunning) "Работает: $serverUrl" else "Сервер выключен",
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

@Suppress("DEPRECATION")
@Composable
private fun WebServerConnectionCard(
    serverUrl: String,
    networkName: String,
    qrBitmap: androidx.compose.ui.graphics.ImageBitmap?,
) {
    val clipboardManager = LocalClipboardManager.current

    val onCopyUrl: () -> Unit = remember(serverUrl, clipboardManager) {
        {
            clipboardManager.setText(AnnotatedString(serverUrl))
            SnackBar.success("Ссылка скопирована в буфер")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SettingsCardColor)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Статус сети
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF00E676))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Сеть: $networkName",
                color = SettingsRowTextSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        // Кликабельный URL
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF25232A))
                .clickable(onClick = onCopyUrl)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = serverUrl,
                color = SettingsAccentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(16.dp))

        // QR-код
        if (qrBitmap != null) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "QR-код для подключения",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Отсканируйте камерой на планшете/ПК",
                color = SettingsRowTextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // Кнопки действий: Копировать и Поделиться
        WebServerActionButtons(
            serverUrl = serverUrl,
            onCopy = onCopyUrl
        )

        Spacer(Modifier.height(16.dp))

        // Пояснение
        Text(
            text = "Компьютер или планшет должен быть подключен к этой же сети Wi-Fi. В браузере будет доступен просмотр видео и скачивание файлов.",
            color = SettingsRowTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun WebServerActionButtons(
    serverUrl: String,
    onCopy: () -> Unit
) {
    val context = LocalContext.current

    val onShare: () -> Unit = remember(serverUrl, context) {
        {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, serverUrl)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Поделиться ссылкой")
            context.startActivity(shareIntent)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onCopy,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = SettingsAccentColor,
                contentColor = Color(0xFF2E2961)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Скопировать", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = SettingsRowTextPrimary)
            Spacer(Modifier.width(6.dp))
            Text("Поделиться", fontSize = 13.sp, color = SettingsRowTextPrimary)
        }
    }
}

