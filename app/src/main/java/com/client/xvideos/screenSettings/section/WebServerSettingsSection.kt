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

private val CONNECTION_CARD_SHAPE = RoundedCornerShape(24.dp)
private val NETWORK_INDICATOR_SHAPE = RoundedCornerShape(5.dp)
private val URL_BOX_SHAPE = RoundedCornerShape(12.dp)
private val QR_BOX_SHAPE = RoundedCornerShape(18.dp)
private val ACTION_BUTTON_SHAPE = RoundedCornerShape(12.dp)
private val NETWORK_ACTIVE_COLOR = Color(0xFF00E676)
private val URL_BOX_BG_COLOR = Color(0xFF25232A)
private val ERROR_TEXT_COLOR = Color(0xFFFF5252)
private val COPY_BUTTON_TEXT_COLOR = Color(0xFF2E2961)
private val QR_BOX_SIZE = 210.dp

private val ERROR_SPACER_HEIGHT = 8.dp
private val SECTION_SPACER_HEIGHT = 16.dp
private val ERROR_HORIZONTAL_PADDING = 24.dp
private val ERROR_FONT_SIZE = 13.sp
private val CONNECTION_CARD_HORIZONTAL_PADDING = 16.dp
private val CONNECTION_CARD_INNER_PADDING = 20.dp
private val NETWORK_INDICATOR_SIZE = 10.dp
private val NETWORK_TEXT_FONT_SIZE = 13.sp
private val NETWORK_SPACER_WIDTH = 8.dp
private val NETWORK_BOTTOM_SPACER_HEIGHT = 14.dp
private val URL_BOX_HORIZONTAL_PADDING = 16.dp
private val URL_BOX_VERTICAL_PADDING = 10.dp
private val URL_TEXT_FONT_SIZE = 17.sp
private val QR_PADDING = 12.dp
private val QR_BOTTOM_SPACER_HEIGHT = 12.dp
private val QR_HINT_FONT_SIZE = 12.sp
private val ACTION_BUTTONS_SPACING = 10.dp
private val ACTION_ICON_SIZE = 16.dp
private val ACTION_ICON_SPACER_WIDTH = 6.dp
private val ACTION_BUTTON_FONT_SIZE = 13.sp
private val FOOTER_HINT_FONT_SIZE = 12.sp
private val FOOTER_HINT_LINE_HEIGHT = 16.sp

private const val TITLE_WEBSERVER = "Веб-сервер Wi-Fi"
private const val TITLE_CONNECTION = "Подключение"
private const val TEXT_STREAM_TO_PC = "Трансляция на ПК"
private const val TEXT_KEEP_AWAKE = "Не усыплять Wi-Fi и процессор"
private const val TEXT_KEEP_AWAKE_SUBTITLE = "Стабильный стриминг при заблокированном экране"
private const val TEXT_SERVER_STOPPED = "Сервер выключен"
private const val TEXT_COPY = "Скопировать"
private const val TEXT_SHARE = "Поделиться"
private const val TEXT_SHARE_CHOOSER = "Поделиться ссылкой"
private const val SERVER_RUNNING_PREFIX = "Работает: "
private const val ERROR_TEXT_PREFIX = "Ошибка: "
private const val NETWORK_NAME_PREFIX = "Сеть: "
private const val QR_CD = "QR-код для подключения"
private const val QR_HINT = "Отсканируйте камерой на планшете/ПК"
private const val FOOTER_HINT_TEXT = "Компьютер или планшет должен быть подключен к этой же сети Wi-Fi. В браузере будет доступен просмотр видео и скачивание файлов."
private const val MSG_CONNECT_WIFI = "Подключитесь к Wi-Fi или включите точку доступа"
private const val MSG_SERVER_STARTING = "Запуск веб-сервера..."
private const val MSG_SERVER_STOPPED = "Веб-сервер остановлен"
private const val MSG_COPIED_TO_CLIPBOARD = "Ссылка скопирована в буфер"

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
                    SnackBar.error(MSG_CONNECT_WIFI)
                } else {
                    WebServerService.start(context, port)
                    SnackBar.info(MSG_SERVER_STARTING)
                }
            } else {
                WebServerService.stop(context)
                SnackBar.info(MSG_SERVER_STOPPED)
            }
        }
    }
    val onKeepAwakeChange: (Boolean) -> Unit = remember {
        { enabled ->
            Settings.web_server_keep_awake.setValue(enabled)
        }
    }

    val serverSubtitle = remember(isRunning, serverUrl) {
        if (isRunning) "$SERVER_RUNNING_PREFIX$serverUrl" else TEXT_SERVER_STOPPED
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle(TITLE_WEBSERVER)

        SettingsGroup {
            SettingsSwitchRow(
                icon = R.drawable.hard_drive_2_24,
                text = TEXT_STREAM_TO_PC,
                subtitle = serverSubtitle,
                value = isRunning,
                onValueChange = onToggleServer
            )

            SettingsDivider()

            SettingsSwitchRow(
                icon = R.drawable.memory_24,
                text = TEXT_KEEP_AWAKE,
                subtitle = TEXT_KEEP_AWAKE_SUBTITLE,
                value = keepAwake,
                onValueChange = onKeepAwakeChange
            )
        }

        if (lastError != null) {
            Spacer(Modifier.height(ERROR_SPACER_HEIGHT))
            Text(
                text = "$ERROR_TEXT_PREFIX$lastError",
                color = ERROR_TEXT_COLOR,
                fontSize = ERROR_FONT_SIZE,
                modifier = Modifier.padding(horizontal = ERROR_HORIZONTAL_PADDING)
            )
        }

        val currentServerUrl = serverUrl
        if (isRunning && currentServerUrl != null) {
            Spacer(Modifier.height(SECTION_SPACER_HEIGHT))
            SettingsSectionTitle(TITLE_CONNECTION)
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
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current

    val onCopyUrl: () -> Unit = remember(serverUrl, clipboardManager) {
        {
            clipboardManager.setText(AnnotatedString(serverUrl))
            SnackBar.success(MSG_COPIED_TO_CLIPBOARD)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CONNECTION_CARD_HORIZONTAL_PADDING)
            .clip(CONNECTION_CARD_SHAPE)
            .background(SettingsCardColor)
            .padding(CONNECTION_CARD_INNER_PADDING),
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
                    .size(NETWORK_INDICATOR_SIZE)
                    .clip(NETWORK_INDICATOR_SHAPE)
                    .background(NETWORK_ACTIVE_COLOR)
            )
            Spacer(Modifier.width(NETWORK_SPACER_WIDTH))
            Text(
                text = "$NETWORK_NAME_PREFIX$networkName",
                color = SettingsRowTextSecondary,
                fontSize = NETWORK_TEXT_FONT_SIZE
            )
        }

        Spacer(Modifier.height(NETWORK_BOTTOM_SPACER_HEIGHT))

        // Кликабельный URL
        Box(
            modifier = Modifier
                .clip(URL_BOX_SHAPE)
                .background(URL_BOX_BG_COLOR)
                .clickable(onClick = onCopyUrl)
                .padding(horizontal = URL_BOX_HORIZONTAL_PADDING, vertical = URL_BOX_VERTICAL_PADDING)
        ) {
            Text(
                text = serverUrl,
                color = SettingsAccentColor,
                fontSize = URL_TEXT_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(SECTION_SPACER_HEIGHT))

        // QR-код
        if (qrBitmap != null) {
            Box(
                modifier = Modifier
                    .size(QR_BOX_SIZE)
                    .clip(QR_BOX_SHAPE)
                    .background(Color.White)
                    .padding(QR_PADDING),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = QR_CD,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(QR_BOTTOM_SPACER_HEIGHT))
            Text(
                text = QR_HINT,
                color = SettingsRowTextSecondary,
                fontSize = QR_HINT_FONT_SIZE
            )
        }

        Spacer(Modifier.height(SECTION_SPACER_HEIGHT))

        // Кнопки действий: Копировать и Поделиться
        WebServerActionButtons(
            serverUrl = serverUrl,
            onCopy = onCopyUrl
        )

        Spacer(Modifier.height(SECTION_SPACER_HEIGHT))

        // Пояснение
        Text(
            text = FOOTER_HINT_TEXT,
            color = SettingsRowTextSecondary,
            fontSize = FOOTER_HINT_FONT_SIZE,
            textAlign = TextAlign.Center,
            lineHeight = FOOTER_HINT_LINE_HEIGHT
        )
    }
}

@Composable
private fun WebServerActionButtons(
    serverUrl: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val onShare: () -> Unit = remember(serverUrl, context) {
        {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, serverUrl)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, TEXT_SHARE_CHOOSER)
            context.startActivity(shareIntent)
        }
    }

    val copyButtonColors = ButtonDefaults.buttonColors(
        containerColor = SettingsAccentColor,
        contentColor = COPY_BUTTON_TEXT_COLOR
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ACTION_BUTTONS_SPACING)
    ) {
        Button(
            onClick = onCopy,
            modifier = Modifier.weight(1f),
            colors = copyButtonColors,
            shape = ACTION_BUTTON_SHAPE
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(ACTION_ICON_SIZE))
            Spacer(Modifier.width(ACTION_ICON_SPACER_WIDTH))
            Text(TEXT_COPY, fontSize = ACTION_BUTTON_FONT_SIZE, fontWeight = FontWeight.Medium)
        }

        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.weight(1f),
            shape = ACTION_BUTTON_SHAPE
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(ACTION_ICON_SIZE), tint = SettingsRowTextPrimary)
            Spacer(Modifier.width(ACTION_ICON_SPACER_WIDTH))
            Text(TEXT_SHARE, fontSize = ACTION_BUTTON_FONT_SIZE, color = SettingsRowTextPrimary)
        }
    }
}
