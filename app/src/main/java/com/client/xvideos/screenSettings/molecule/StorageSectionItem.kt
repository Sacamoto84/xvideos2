package com.client.xvideos.screenSettings.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.atom.StorageFolderRow
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.StorageFolderStat
import com.client.xvideos.screenSettings.components.StorageProgressRow
import com.client.xvideos.screenSettings.components.StorageStat

/**
 * Раздел статистики: строка раздела и под ней, пока [expanded], его папки.
 * Всё в одном блоке группы. Раздел без папок не раскрывается.
 */
@Composable
internal fun StorageSectionItem(
    stat: StorageStat,
    progress: Float,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasFolders = stat.folders.isNotEmpty()
    Column(modifier = modifier) {
        StorageProgressRow(
            stat = stat,
            progress = progress,
            expanded = expanded,
            onClick = if (hasFolders) onToggle else null
        )
        AnimatedVisibility(
            visible = expanded && hasFolders,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column {
                stat.folders.forEach { folder ->
                    key(folder.name) {
                        SettingsDivider()
                        val folderProgress = if (stat.sizeBytes > 0L) {
                            (folder.sizeBytes.toFloat() / stat.sizeBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        StorageFolderRow(folder = folder, progress = folderProgress)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun StorageSectionItemPreview() = SettingsPreview {
    StorageSectionItem(
        stat = StorageStat(
            key = "R",
            title = "R",
            sizeBytes = 500_000_000,
            fileCount = 200,
            folders = listOf(
                StorageFolderStat(name = "Download", sizeBytes = 450_000_000, fileCount = 150),
                StorageFolderStat(name = null, sizeBytes = 50_000_000, fileCount = 50)
            )
        ),
        progress = 0.5f,
        expanded = true,
        onToggle = {}
    )
}
