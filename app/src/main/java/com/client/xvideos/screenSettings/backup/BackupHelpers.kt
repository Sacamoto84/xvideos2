package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.backup.XlrBackupContentMode

internal val BACKUP_SELECTOR_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .height(56.dp)
    .padding(horizontal = 16.dp, vertical = 6.dp)

internal const val TEXT_PASSWORD_LABEL = "Пароль архива"
internal const val TEXT_PASSWORD_HIDE = "Скрыть пароль"
internal const val TEXT_PASSWORD_SHOW = "Показать пароль"

internal fun backupContentModeTitle(mode: XlrBackupContentMode): String {
    return when (mode) {
        XlrBackupContentMode.MINI -> "Мини"
        XlrBackupContentMode.FULL -> "Полный"
    }
}

/**
 * Строки консоли для показа: у пустой — заглушка, многострочная запись — по
 * строке на элемент.
 */
internal fun backupConsoleLines(lines: List<String>): State<List<String>> = derivedStateOf {
    lines
        .ifEmpty { listOf("Пока пусто") }
        .flatMap { entry -> entry.lineSequence().toList() }
}
