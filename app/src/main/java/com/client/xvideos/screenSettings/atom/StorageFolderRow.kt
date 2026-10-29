package com.client.xvideos.screenSettings.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.screenSettings.components.SettingsDividerColor
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsRowTextSecondary
import com.client.xvideos.screenSettings.components.StorageFolderStat
import com.client.xvideos.screenSettings.components.WhatsAppGreen

/**
 * Папка раздела в раскрытой статистике: имя, размер, число файлов и доля
 * папки в разделе ([progress]). Отступы совпадают с текстом и полосой
 * строки раздела над ней.
 */
@Composable
internal fun StorageFolderRow(
    folder: StorageFolderStat,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val formattedSize = remember(folder.sizeBytes) { formatBytes(folder.sizeBytes) }
    // Слева — под текстом строки раздела (16 + иконка 24 + 16), справа — до шеврона (16 + 24 + 12).
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 52.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = folder.name ?: "Файлы в корне",
                color = SettingsRowTextPrimary,
                style = Theme.L.Type.rowSubtitle.copy(color = SettingsRowTextPrimary)
            )
            Text(
                text = formattedSize,
                color = SettingsRowTextSecondary,
                style = Theme.L.Type.rowSubtitle.copy(color = SettingsRowTextSecondary)
            )
        }
        Spacer(Modifier.height(6.dp))
        val progressProvider: () -> Float = remember(progress) { { progress } }
        LinearProgressIndicator(
            progress = progressProvider,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = WhatsAppGreen,
            trackColor = SettingsDividerColor
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "файлов: ${folder.fileCount}",
            color = SettingsRowTextSecondary,
            style = Theme.L.Type.caption.copy(color = SettingsRowTextSecondary)
        )
    }
}

@Preview
@Composable
private fun StorageFolderRowPreview() {
    StorageFolderRow(
        folder = StorageFolderStat(name = "Download", sizeBytes = 450_000_000, fileCount = 150),
        progress = 0.9f
    )
}
