package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R
import com.client.xvideos.screenSettings.components.SettingsDestructiveColor
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsTopBarColor

@Composable
internal fun BackupConsole(
    lines: List<String>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    consoleSeq: Int = lines.size,
    consoleBaseIndex: Int = 0,
) {
    val visibleLines by remember(lines) { backupConsoleLines(lines) }
    val listState = rememberLazyListState()

    LaunchedEffect(consoleSeq, visibleLines.size) {
        if (visibleLines.isNotEmpty()) {
            listState.scrollToItem(visibleLines.lastIndex)
        }
    }

    Column(modifier = modifier) {
        SettingsListItem(
            icon = R.drawable.hard_drive_2_24,
            text = "Консоль",
            subtitle = if (lines.isEmpty()) "Здесь будет процесс восстановления файлов из сети" else "${visibleLines.size} строк",
            trailing = {
                TextButton(
                    enabled = lines.isNotEmpty(),
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(contentColor = SettingsDestructiveColor)
                ) {
                    Text("Очистить")
                }
            }
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .background(SettingsTopBarColor, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            items(
                count = visibleLines.size,
                key = { index -> "${consoleBaseIndex + index}:${visibleLines[index].hashCode()}" }
            ) { index ->
                BackupConsoleLine(visibleLines[index])
            }
        }
    }
}

@Preview
@Composable
private fun BackupConsolePreview() {
    BackupConsole(
        lines = listOf(
            "---------",
            "R итог",
            "Info: всего 50, неполных 2",
            "Скачано/очередь: видео 10, preview 15",
            "Пропущено: нет video URL 0",
            "Ошибки: битых info 0"
        ),
        onClear = {}
    )
}
