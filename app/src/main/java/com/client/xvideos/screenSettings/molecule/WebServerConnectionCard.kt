package com.client.xvideos.screenSettings.molecule

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsCardColor
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsRowTextSecondary

@Suppress("DEPRECATION")
@Composable
internal fun WebServerConnectionCard(
    serverUrl: String,
    networkName: String,
    qrBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current

    val onCopyUrl: () -> Unit = remember(serverUrl, clipboardManager) {
        {
            clipboardManager.setText(AnnotatedString(serverUrl))
            SnackBar.success("Ссылка скопирована в буфер")
        }
    }

    val networkLabel = remember(networkName) { "Сеть: $networkName" }

    val cardBaseModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(SettingsCardColor)
        .padding(20.dp)

    Column(
        modifier = if (modifier == Modifier) cardBaseModifier else modifier.then(cardBaseModifier),
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
                text = networkLabel,
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clickable(onClick = onCopyUrl)
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

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun WebServerConnectionCardPreview() = SettingsPreview {
    WebServerConnectionCard(serverUrl = "http://192.168.0.10:8080", networkName = "Wi-Fi", qrBitmap = null)
}
