package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.backup.XlrRestoreMode
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Вопрос перед восстановлением: что делать с тем, чего в архиве нет.
 *
 * Раньше восстановление умело только заменять и об этом не спрашивало:
 * сохранённое после создания бэкапа пропадало, а добавить старый архив к
 * текущим данным было нечем.
 *
 * @param summary Что выбрано в архиве: число файлов и размер.
 * @param onSelected Режим выбран; диалог закрывает вызывающий.
 */
@Composable
internal fun BackupRestoreModeDialog(
    summary: String,
    onDismiss: () -> Unit,
    onSelected: (XlrRestoreMode) -> Unit,
) {
    LavenderDialog(
        title = "Как восстановить?",
        onDismiss = onDismiss,
        content = {
            val dialogTheme = Theme.DialogLavande
            val bodyStyle = Theme.L.Type.dialogBody.copy(color = dialogTheme.bodyColor)
            val buttonShape = RoundedCornerShape(dialogTheme.buttonBorderRadius)

            Text(
                text = "Из архива: $summary. DB, настройки и кеши не трогаются.",
                style = bodyStyle,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSelected(XlrRestoreMode.MERGE) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = dialogTheme.buttonBackground,
                    contentColor = dialogTheme.buttonTextColor
                ),
                shape = buttonShape
            ) {
                Text("Объединить", color = dialogTheme.buttonTextColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "К тому, что есть сейчас, добавится содержимое архива. " +
                    "Запись, которая есть и там и там, берётся из архива.",
                style = bodyStyle,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSelected(XlrRestoreMode.REPLACE) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = dialogTheme.buttonBackgroundDestructive,
                    contentColor = dialogTheme.buttonTextColor
                ),
                shape = buttonShape
            ) {
                Text("Заменить", color = dialogTheme.buttonTextColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "В выбранных папках останется только то, что есть в архиве. " +
                    "Сохранённое после создания бэкапа пропадёт.",
                style = bodyStyle,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun BackupRestoreModeDialogPreview() = SettingsPreview {
    BackupRestoreModeDialog(summary = "214 файлов, 3.1 МБ", onDismiss = {}, onSelected = {})
}
