package com.client.xvideos.screenSettings.molecule

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.screenSettings.atom.SettingsChevron
import com.client.xvideos.screenSettings.components.SettingsCardColor
import com.client.xvideos.screenSettings.components.SettingsDividerColor
import com.client.xvideos.screenSettings.components.SettingsIcon
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsRowTextSecondary
import com.client.xvideos.screenSettings.components.StorageStat
import com.client.xvideos.screenSettings.components.WhatsAppGreen

/**
 * Строка раздела. С [onClick] справа шеврон: он поворачивается вниз, пока
 * список папок раздела раскрыт ([expanded]).
 */
@Composable
internal fun StorageProgressRow(
    stat: StorageStat,
    progress: Float,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val formattedSize = remember(stat.sizeBytes) { formatBytes(stat.sizeBytes) }
    val subtitleText = remember(stat.key, stat.fileCount) {
        "${sectionSubtitle(stat.key)} \u2022 файлов: ${stat.fileCount}"
    }
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val rowModifier = modifier
        .fillMaxWidth()
        .background(SettingsCardColor)
        .then(clickModifier)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(storageIcon(stat.key))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.title,
                    color = SettingsRowTextPrimary,
                    style = Theme.L.Type.rowTitle.copy(color = SettingsRowTextPrimary)
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
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = WhatsAppGreen,
                trackColor = SettingsDividerColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitleText,
                color = SettingsRowTextSecondary,
                style = Theme.L.Type.caption.copy(color = SettingsRowTextSecondary)
            )
        }
        if (onClick != null) {
            val chevronRotation by animateFloatAsState(
                targetValue = if (expanded) 90f else 0f,
                label = "storageChevron"
            )
            Spacer(Modifier.width(12.dp))
            SettingsChevron(Modifier.graphicsLayer { rotationZ = chevronRotation })
        }
    }
}

@DrawableRes
private fun storageIcon(key: String): Int = when (key) {
    "X" -> R.drawable.icon_xvideos_mini
    "L" -> R.drawable.icon_luscious
    "XLive" -> R.drawable.icon_xlive
    else -> R.drawable.icon_red
}

private fun sectionSubtitle(key: String): String = when (key) {
    "X" -> "XVideos"
    "L" -> "Luscious"
    "XLive" -> "Мои, История, Записи"
    else -> "RedGifs"
}

@Preview
@Composable
private fun StorageProgressRowPreview() {
    StorageProgressRow(
        stat = StorageStat("R", "R", 500_000_000, 200),
        progress = 0.5f,
        modifier = Modifier,
        expanded = true,
        onClick = {}
    )
}
