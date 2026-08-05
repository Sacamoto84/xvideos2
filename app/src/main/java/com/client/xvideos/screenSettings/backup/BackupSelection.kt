package com.client.xvideos.screenSettings.backup

import com.client.xvideos.R

import androidx.annotation.DrawableRes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState

import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupReport
import com.client.xvideos.common.util.formatBytes

internal fun initialSectionSelection(items: List<XlrBackupItem>): Set<String> {
    return items
        .filter { item -> item.parentPath == null && (item.files > 0 || item.bytes > 0L) }
        .mapTo(mutableSetOf()) { it.path }
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
        val children = items.filter { it.parentPath == item.path }.map { it.path }
        selected.remove(item.path)
        selected.removeAll(children)
        if (!checked) selected.add(item.path)
        return selected
    }

    val siblings = items.filter { it.parentPath == parentPath }.map { it.path }
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
        selected.removeAll(siblings)
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
    if (item.parentPath != null && item.parentPath in selectedPaths) return true
    if (item.parentPath == null) {
        val children = items.filter { it.parentPath == item.path }
        return children.isNotEmpty() && children.all { it.path in selectedPaths }
    }
    return false
}

internal fun backupSectionToggleState(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    section: XlrBackupItem
): ToggleableState {
    if (section.path in selectedPaths) return ToggleableState.On
    val children = items.filter { it.parentPath == section.path }
    if (children.isEmpty()) return ToggleableState.Off
    val selectedChildren = children.count { it.path in selectedPaths }
    return when {
        selectedChildren == 0 -> ToggleableState.Off
        selectedChildren == children.size -> ToggleableState.On
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
