package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsValueRow

@Composable
internal fun BackupFolderList(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Column(modifier = modifier) {
            SettingsValueRow(
                icon = R.drawable.hard_drive_2_24,
                text = "Папки",
                value = "Нет данных для backup"
            )
        }
        return
    }

    var expandedSections by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val rootItems = remember(items) { items.filter { it.parentPath == null } }
    val childrenByParent = remember(items) {
        items.filter { it.parentPath != null }.groupBy { it.parentPath }
    }

    Column(modifier = modifier) {
        rootItems.forEachIndexed { index, section ->
            val children = childrenByParent[section.path].orEmpty()
            if (index > 0) SettingsDivider()
            BackupSectionGroup(
                section = section,
                children = children,
                items = items,
                selectedPaths = selectedPaths,
                expanded = section.path in expandedSections,
                enabled = enabled,
                onToggleExpanded = {
                    expandedSections = if (section.path in expandedSections) {
                        expandedSections - section.path
                    } else {
                        expandedSections + section.path
                    }
                },
                onToggle = onToggle
            )
        }
    }
}

@Preview
@Composable
private fun BackupFolderListPreview() {
    val items = listOf(
        XlrBackupItem(section = "X", path = "X", title = "XVideos", parentPath = null, files = 120, bytes = 300_000_000L),
        XlrBackupItem(section = "X", path = "X/Favorites", title = "Избранное", parentPath = "X", files = 100, bytes = 250_000_000L),
        XlrBackupItem(section = "L", path = "L", title = "Luscious", parentPath = null, files = 45, bytes = 80_000_000L)
    )
    var selected by remember { mutableStateOf(setOf("X", "X/Favorites", "L")) }
    BackupFolderList(
        items = items,
        selectedPaths = selected,
        enabled = true,
        onToggle = { path ->
            selected = if (path in selected) selected - path else selected + path
        }
    )
}
