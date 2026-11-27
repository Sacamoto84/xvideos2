package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary

@Composable
internal fun BackupConsoleLine(
    line: String,
    modifier: Modifier = Modifier,
) {
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
    val isWarning = lower.contains("пропущено") || lower.contains("нет ")
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
        modifier = modifier.padding(vertical = if (isSummary) 2.dp else 1.dp)
    )
}

@Preview
@Composable
private fun BackupConsoleLinePreview() {
    BackupConsoleLine("Скачано/очередь: видео 10, preview 15")
}
