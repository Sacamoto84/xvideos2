package com.client.xvideos.screenSettings.backup

import com.client.xvideos.R

import androidx.annotation.DrawableRes

import androidx.compose.ui.state.ToggleableState

import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupReport
import com.client.xvideos.common.util.formatBytes

internal fun initialSectionSelection(items: List<XlrBackupItem>): Set<String> {
    val result = mutableSetOf<String>()
    for (item in items) {
        if (item.parentPath == null && (item.files > 0 || item.bytes > 0L)) {
            result.add(item.path)
        }
    }
    return result
}

internal fun toggleBackupPath(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    path: String
): Set<String> {
    val item = items.firstOrNull { it.path == path } ?: return selectedPaths
    val selected = selectedPaths.toMutableSet()
    val checked = isBackupPathChecked(items, selectedPaths, item)

    // Читаем свойство один раз в локальную переменную: после выноса модели в
    // :core умного приведения к String по проверке `item.parentPath == null`
    // компилятор уже не делает — свойство из другого модуля.
    val parentPath = item.parentPath
    if (parentPath == null) {
        selected.remove(item.path)
        for (candidate in items) {
            if (candidate.parentPath == item.path) {
                selected.remove(candidate.path)
            }
        }
        if (!checked) selected.add(item.path)
        return selected
    }

    val siblings = mutableListOf<String>()
    for (candidate in items) {
        if (candidate.parentPath == parentPath) {
            siblings.add(candidate.path)
        }
    }
    if (parentPath in selected) {
        selected.remove(parentPath)
        selected.addAll(siblings)
    }

    if (checked) {
        selected.remove(item.path)
    } else {
        selected.add(item.path)
    }

    if (siblings.isNotEmpty() && siblings.all { it in selected }) {
        selected.removeAll(siblings.toSet())
        selected.add(parentPath)
    }

    return selected
}

internal fun isBackupPathChecked(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    item: XlrBackupItem
): Boolean {
    if (item.path in selectedPaths) return true
    val parentPath = item.parentPath
    if (parentPath != null && parentPath in selectedPaths) return true
    if (parentPath == null) {
        var hasChildren = false
        for (candidate in items) {
            if (candidate.parentPath == item.path) {
                hasChildren = true
                if (candidate.path !in selectedPaths) return false
            }
        }
        return hasChildren
    }
    return false
}

internal fun backupSectionToggleState(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    section: XlrBackupItem
): ToggleableState {
    if (section.path in selectedPaths) return ToggleableState.On
    var childCount = 0
    var selectedCount = 0
    for (candidate in items) {
        if (candidate.parentPath == section.path) {
            childCount++
            if (candidate.path in selectedPaths) {
                selectedCount++
            }
        }
    }
    if (childCount == 0) return ToggleableState.Off
    return when (selectedCount) {
        0 -> ToggleableState.Off
        childCount -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
}

internal fun selectionSummaryText(report: XlrBackupReport): String {
    return "${report.files} файлов • ${formatBytes(report.bytes)}"
}

internal fun backupItemTitle(item: XlrBackupItem): String {
    return if (item.parentPath == null) item.title else "  ${item.title}"
}

@DrawableRes
internal fun backupItemIcon(section: String): Int {
    return when (section) {
        "X" -> R.drawable.icon_xvideos_white
        "L" -> R.drawable.icon_luscious
        "R" -> R.drawable.icon_red
        else -> R.drawable.hard_drive_2_24
    }
}
