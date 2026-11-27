package com.client.xvideos.screenSettings.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.screenSettings.components.StorageFolderStat
import com.client.xvideos.screenSettings.components.StorageStat
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun StorageStatisticsSection(
    stats: List<StorageStat>,
    modifier: Modifier = Modifier
) {
    val totalBytes = remember(stats) { stats.sumOf { it.sizeBytes } }
    val formattedTotal = remember(totalBytes) { formatBytes(totalBytes) }
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

@Preview
@Composable
private fun StorageStatisticsSectionPreview() {
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
