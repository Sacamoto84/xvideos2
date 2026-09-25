package com.client.xvideos.screenSettings.components

import com.client.xvideos.common.theme.Theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.util.formatBytes
import java.io.File

import kotlinx.collections.immutable.persistentListOf

private val PROGRESS_CORNER = 6.dp
private val PROGRESS_SHAPE = RoundedCornerShape(PROGRESS_CORNER)
private const val TEXT_TOTAL_DATA = "Всего данных"
private const val TEXT_FILES_COUNT_PREFIX = " \u2022 файлов: "
private const val SUBTITLE_X = "XVideos"
private const val SUBTITLE_L = "Luscious"
private const val SUBTITLE_R = "RedGifs"
private val STORAGE_ROW_HORIZONTAL_PADDING = 16.dp
private val STORAGE_ROW_VERTICAL_PADDING = 12.dp
private val PROGRESS_BAR_HEIGHT = 6.dp
private val ICON_SPACER_WIDTH = 16.dp
private val SUBTITLE_SPACER_HEIGHT = 4.dp
private const val PROGRESS_COERCE_MIN = 0f
private const val PROGRESS_COERCE_MAX = 1f
private val PROGRESS_BAR_MODIFIER = Modifier
    .fillMaxWidth()
    .height(PROGRESS_BAR_HEIGHT)
    .clip(PROGRESS_SHAPE)
private val STORAGE_ROW_TITLE_STYLE = Theme.L.Type.rowTitle.copy(color = SettingsRowTextPrimary)
private val STORAGE_ROW_SUBTITLE_STYLE = Theme.L.Type.rowSubtitle.copy(color = SettingsRowTextSecondary)
private val STORAGE_CAPTION_STYLE = Theme.L.Type.caption.copy(color = SettingsRowTextSecondary)

@Immutable
internal data class StorageStat(
    val key: String,
    val title: String,
    val sizeBytes: Long = 0L,
    val fileCount: Int = 0
)

@Immutable
internal data class FolderSnapshot(
    val sizeBytes: Long,
    val fileCount: Int
)

internal val EmptyStorageStats: List<StorageStat> = persistentListOf(
    StorageStat(key = "X", title = "X"),
    StorageStat(key = "L", title = "L"),
    StorageStat(key = "R", title = "R")
)

@Composable
internal fun StorageStatisticsSection(
    stats: List<StorageStat>,
    modifier: Modifier = Modifier
) {
    val totalBytes = remember(stats) { stats.sumOf { it.sizeBytes } }
    val formattedTotal = remember(totalBytes) { formatBytes(totalBytes) }
    SettingsGroup(modifier = modifier) {
        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = TEXT_TOTAL_DATA,
            value = formattedTotal
        )

        stats.forEach { stat ->
            key(stat.key) {
                SettingsDivider()
                val progress = if (totalBytes > 0L) {
                    (stat.sizeBytes.toFloat() / totalBytes.toFloat()).coerceIn(PROGRESS_COERCE_MIN, PROGRESS_COERCE_MAX)
                } else {
                    0f
                }
                StorageProgressRow(stat = stat, progress = progress)
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
            StorageStat("R", "R", 500_000_000, 200)
        )
    )
}

@Composable
internal fun StorageProgressRow(
    stat: StorageStat,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val formattedSize = remember(stat.sizeBytes) { formatBytes(stat.sizeBytes) }
    val subtitleText = remember(stat.key, stat.fileCount) {
        "${sectionSubtitle(stat.key)}$TEXT_FILES_COUNT_PREFIX${stat.fileCount}"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SettingsCardColor)
            .padding(horizontal = STORAGE_ROW_HORIZONTAL_PADDING, vertical = STORAGE_ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(storageIcon(stat.key))
        Spacer(Modifier.width(ICON_SPACER_WIDTH))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.title,
                    color = SettingsRowTextPrimary,
                    style = STORAGE_ROW_TITLE_STYLE
                )
                Text(
                    text = formattedSize,
                    color = SettingsRowTextSecondary,
                    style = STORAGE_ROW_SUBTITLE_STYLE
                )
            }
            Spacer(Modifier.height(PROGRESS_BAR_HEIGHT))
            LinearProgressIndicator(
                progress = { progress },
                modifier = PROGRESS_BAR_MODIFIER,
                color = WhatsAppGreen,
                trackColor = SettingsDividerColor
            )
            Spacer(Modifier.height(SUBTITLE_SPACER_HEIGHT))
            Text(
                text = subtitleText,
                color = SettingsRowTextSecondary,
                style = STORAGE_CAPTION_STYLE
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun StorageProgressRowPreview() = SettingsPreview {
    StorageProgressRow(
        stat = StorageStat("R", "R", 500_000_000, 200),
        progress = 0.5f
    )
}

@DrawableRes
private fun storageIcon(key: String): Int {
    return when (key) {
        "X" -> R.drawable.icon_xvideos_mini
        "L" -> R.drawable.icon_luscious
        else -> R.drawable.icon_red
    }
}

private fun sectionSubtitle(key: String): String {
    return when (key) {
        "X" -> SUBTITLE_X
        "L" -> SUBTITLE_L
        else -> SUBTITLE_R
    }
}

internal fun loadStorageStats(): List<StorageStat> {
    return listOf(
        "X" to File(AppPath.main, "X"),
        "L" to File(AppPath.main, "L"),
        "R" to File(AppPath.main, "R")
    ).map { (key, folder) ->
        val snapshot = folder.collectSnapshot()
        StorageStat(
            key = key,
            title = key,
            sizeBytes = snapshot.sizeBytes,
            fileCount = snapshot.fileCount
        )
    }
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
