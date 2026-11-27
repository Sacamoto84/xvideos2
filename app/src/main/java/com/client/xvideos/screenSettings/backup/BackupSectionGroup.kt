package com.client.xvideos.screenSettings.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsValueRow

@Composable
internal fun BackupSectionGroup(
    section: XlrBackupItem,
    children: List<XlrBackupItem>,
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    expanded: Boolean,
    enabled: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = backupSectionToggleState(items, selectedPaths, section)

    Column(modifier = modifier) {
        SettingsListItem(
            icon = backupItemIcon(section.section),
            text = section.title,
            subtitle = "${section.files} файлов • ${formatBytes(section.bytes)}",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TriStateCheckbox(
                        state = state,
                        enabled = enabled,
                        onClick = { onToggle(section.path) }
                    )
                    TextButton(
                        enabled = enabled,
                        onClick = onToggleExpanded
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.exo_ic_chevron_right),
                            contentDescription = if (expanded) "Свернуть" else "Развернуть",
                            tint = SettingsAccentColor,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (expanded) 90f else 0f
                            }
                        )
                    }
                }
            },
            onClick = { if (enabled) onToggleExpanded() }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column {
                if (children.isEmpty()) {
                    SettingsValueRow(
                        icon = backupItemIcon(section.section),
                        text = section.title,
                        value = "Нет вложенных папок"
                    )
                } else {
                    children.forEach { child ->
                        SettingsDivider()
                        SettingsListItem(
                            icon = backupItemIcon(child.section),
                            text = backupItemTitle(child),
                            subtitle = "${child.path} • ${child.files} файлов • ${formatBytes(child.bytes)}",
                            trailing = {
                                Checkbox(
                                    checked = isBackupPathChecked(items, selectedPaths, child),
                                    enabled = enabled,
                                    onCheckedChange = { onToggle(child.path) }
                                )
                            },
                            onClick = { if (enabled) onToggle(child.path) }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BackupSectionGroupPreview() {
    val section = XlrBackupItem(section = "X", path = "X", title = "XVideos", parentPath = null, files = 10, bytes = 1_024L)
    BackupSectionGroup(
        section = section,
        children = emptyList(),
        items = listOf(section),
        selectedPaths = setOf("X"),
        expanded = false,
        enabled = true,
        onToggleExpanded = {},
        onToggle = {}
    )
}
