package com.client.xvideos.screenSettings.backup

import com.client.xvideos.R
import com.client.xvideos.common.theme.Theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.dp
import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsCardColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsDividerColor
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsScreenBackground
import com.client.xvideos.screenSettings.components.SettingsTopBarColor
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.common.util.formatBytes

@Composable
internal fun BackupModeSelector(
    selected: BackupFlowScreen,
    enabled: Boolean,
    onSelected: (BackupFlowScreen) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        BackupFlowScreen.entries.forEachIndexed { index, item ->
            SegmentedButton(
                enabled = enabled,
                selected = selected == item,
                onClick = { onSelected(item) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = BackupFlowScreen.entries.size,
                    baseShape = RoundedCornerShape(8.dp)
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = SettingsAccentColor,
                    activeContentColor = SettingsScreenBackground,
                    activeBorderColor = SettingsAccentColor,
                    inactiveContainerColor = SettingsCardColor,
                    inactiveContentColor = Theme.L.textColor,
                    inactiveBorderColor = SettingsDividerColor
                ),
                label = {
                    Text(
                        text = item.title,
                        color = if (selected == item) SettingsScreenBackground else SettingsRowTextPrimary,
                        style = Theme.L.Type.button
                    )
                }
            )
        }
    }
}

@Composable
internal fun BackupContentModeSelector(
    title: String,
    value: XlrBackupContentMode,
    enabled: Boolean,
    description: String,
    onValueChange: (XlrBackupContentMode) -> Unit
) {
    SettingsValueRow(
        icon = R.drawable.hard_drive_2_24,
        text = title,
        value = "${backupContentModeTitle(value)} • $description"
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        XlrBackupContentMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                enabled = enabled,
                selected = value == mode,
                onClick = { onValueChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = XlrBackupContentMode.entries.size
                ),
                label = { Text(backupContentModeTitle(mode)) }
            )
        }
    }
}

internal fun backupContentModeTitle(mode: XlrBackupContentMode): String {
    return when (mode) {
        XlrBackupContentMode.MINI -> "Мини"
        XlrBackupContentMode.FULL -> "Полный"
    }
}

@Composable
internal fun BackupConsole(
    lines: List<String>,
    onClear: () -> Unit
) {
    val visibleLines = lines
        .ifEmpty { listOf("Пока пусто") }
        .flatMap { entry -> entry.lineSequence().toList() }

    SettingsListItem(
        icon = R.drawable.hard_drive_2_24,
        text = "Консоль backup",
        subtitle = if (lines.isEmpty()) "Здесь будет процесс восстановления файлов из сети" else "${visibleLines.size} строк",
        trailing = {
            TextButton(
                enabled = lines.isNotEmpty(),
                onClick = onClear
            ) {
                Text("Очистить", color = SettingsAccentColor)
            }
        }
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(SettingsTopBarColor, RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        Column {
            visibleLines.forEach { line ->
                BackupConsoleLine(line)
            }
        }
    }
}

@Composable
internal fun BackupConsoleLine(line: String) {
    val lower = line.lowercase()
    val isSummary = line.startsWith("---------") || lower.contains("итог")
    val isError = lower.contains("ошиб") ||
            lower.contains("бит") ||
            lower.contains("не скачан") ||
            lower.contains("failed")
    val isSuccess = lower.contains("скачано") ||
            lower.contains("очередь") ||
            lower.contains("готов") ||
            lower.contains("создан") ||
            lower.contains("восстановлен")
    val isWarning = lower.contains("пропущено") ||
            lower.contains("нет ")
    val color = when {
        isSummary -> SettingsAccentColor
        isError -> Theme.L.r0
        isSuccess -> Theme.L.g0
        isWarning -> Theme.L.lavender
        else -> SettingsRowTextPrimary
    }
    val style = if (isSummary) {
        Theme.L.Type.rowTitle.copy(color = color)
    } else {
        Theme.L.Type.rowSubtitle.copy(color = color)
    }

    Text(
        text = line,
        color = color,
        style = style,
        modifier = Modifier.padding(vertical = if (isSummary) 2.dp else 1.dp)
    )
}

@Composable
internal fun BackupSelectionActions(
    enabled: Boolean,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            enabled = enabled,
            onClick = onSelectAll
        ) {
            Text("Все X/L/R")
        }
        Spacer(Modifier.width(8.dp))
        TextButton(
            enabled = enabled,
            onClick = onSelectNone
        ) {
            Text("Снять", color = SettingsAccentColor)
        }
    }
}

@Composable
internal fun BackupFolderList(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit
) {
    if (items.isEmpty()) {
        SettingsValueRow(
            icon = R.drawable.hard_drive_2_24,
            text = "Папки",
            value = "Нет данных для backup"
        )
        return
    }

    var expandedSections by rememberSaveable { mutableStateOf(emptyList<String>()) }

    items
        .filter { it.parentPath == null }
        .forEachIndexed { index, section ->
            val children = items.filter { it.parentPath == section.path }
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

@Composable
internal fun BackupSectionGroup(
    section: XlrBackupItem,
    children: List<XlrBackupItem>,
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    expanded: Boolean,
    enabled: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (String) -> Unit
) {
    val state = backupSectionToggleState(items, selectedPaths, section)

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
                        modifier = Modifier.rotate(if (expanded) 90f else 0f)
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
