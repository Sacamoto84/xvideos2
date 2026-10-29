package com.client.xvideos.screenSettings.components

import com.client.xvideos.common.theme.Theme

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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.screenSettings.atom.SettingsChevron
import com.client.xvideos.screenSettings.molecule.StorageSectionItem
import java.io.File

import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class StorageStat(
    val key: String,
    val title: String,
    val sizeBytes: Long = 0L,
    val fileCount: Int = 0,
    val folders: List<StorageFolderStat> = emptyList()
)

/** Папка первого уровня раздела; `name == null` — файлы прямо в папке раздела. */
@Immutable
internal data class StorageFolderStat(
    val name: String?,
    val sizeBytes: Long,
    val fileCount: Int
)

@Immutable
internal data class FolderSnapshot(
    val sizeBytes: Long,
    val fileCount: Int
)

internal val EmptyStorageStats: List<StorageStat> = persistentListOf(
    StorageStat(key = "X", title = "X"),
    StorageStat(key = "L", title = "L"),
    StorageStat(key = "R", title = "R"),
    StorageStat(key = "XLive", title = "XLive")
)

@Composable
internal fun StorageStatisticsSection(
    stats: List<StorageStat>,
    modifier: Modifier = Modifier
) {
    val totalBytes = remember(stats) { stats.sumOf { it.sizeBytes } }
    val formattedTotal = remember(totalBytes) { formatBytes(totalBytes) }
    // Раскрытые разделы переживают пересчёт статистики и поворот экрана.
    var expandedKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
    SettingsGroup(modifier = modifier) {
        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Всего данных",
            value = formattedTotal
        )

        stats.forEach { stat ->
            key(stat.key) {
                SettingsDivider()
                val progress = if (totalBytes > 0L) {
                    (stat.sizeBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val expanded = stat.key in expandedKeys
                val onToggle = remember(stat.key) {
                    {
                        expandedKeys = if (stat.key in expandedKeys) {
                            expandedKeys - stat.key
                        } else {
                            expandedKeys + stat.key
                        }
                    }
                }
                StorageSectionItem(
                    stat = stat,
                    progress = progress,
                    expanded = expanded,
                    onToggle = onToggle
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun StorageStatisticsSectionPreview() = SettingsPreview {
    StorageStatisticsSection(
        stats = persistentListOf(
            StorageStat("X", "X", 300_000_000, 120),
            StorageStat("L", "L", 200_000_000, 80),
            StorageStat(
                key = "R",
                title = "R",
                sizeBytes = 500_000_000,
                fileCount = 200,
                folders = persistentListOf(
                    StorageFolderStat("Download", 450_000_000, 150),
                    StorageFolderStat("Likes", 50_000_000, 50)
                )
            )
        ),
        modifier = Modifier
    )
}

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

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun StorageProgressRowPreview() = SettingsPreview {
    StorageProgressRow(
        stat = StorageStat("R", "R", 500_000_000, 200),
        progress = 0.5f,
        modifier = Modifier,
        expanded = true,
        onClick = {}
    )
}

@DrawableRes
private fun storageIcon(key: String): Int {
    return when (key) {
        "X" -> R.drawable.icon_xvideos_mini
        "L" -> R.drawable.icon_luscious
        "XLive" -> R.drawable.icon_xlive
        else -> R.drawable.icon_red
    }
}

private fun sectionSubtitle(key: String): String {
    return when (key) {
        "X" -> "XVideos"
        "L" -> "Luscious"
        "XLive" -> "Мои, История, Записи"
        else -> "RedGifs"
    }
}

/**
 * Размер разделов в корне хранилища [root] и их папок первого уровня;
 * XLive — свой раздел, с записями эфиров.
 */
internal fun loadStorageStats(root: File = File(AppPath.main)): List<StorageStat> {
    return listOf("X", "L", "R", "XLive").map { key ->
        val folders = File(root, key).collectFolders()
        StorageStat(
            key = key,
            title = key,
            sizeBytes = folders.sumOf { it.sizeBytes },
            fileCount = folders.sumOf { it.fileCount },
            folders = folders
        )
    }
}

/** Папки первого уровня, большие первыми; файлы прямо в разделе — последней строкой. */
private fun File.collectFolders(): List<StorageFolderStat> {
    val children = listFiles() ?: return emptyList()
    val folders = children
        .filter { it.isDirectory }
        .map { dir ->
            val snapshot = dir.collectSnapshot()
            StorageFolderStat(name = dir.name, sizeBytes = snapshot.sizeBytes, fileCount = snapshot.fileCount)
        }
        .sortedWith(compareByDescending<StorageFolderStat> { it.sizeBytes }.thenBy { it.name })
    val rootFiles = children.filter { it.isFile }
    if (rootFiles.isEmpty()) return folders
    return folders + StorageFolderStat(
        name = null,
        sizeBytes = rootFiles.sumOf { it.length() },
        fileCount = rootFiles.size
    )
}

private fun File.collectSnapshot(): FolderSnapshot {
    if (!exists()) return FolderSnapshot(sizeBytes = 0L, fileCount = 0)

    var sizeBytes = 0L
    var fileCount = 0
    walkTopDown().forEach { file ->
        if (file.isFile) {
            sizeBytes += file.length()
            fileCount += 1
        }
    }
    return FolderSnapshot(sizeBytes = sizeBytes, fileCount = fileCount)
}
