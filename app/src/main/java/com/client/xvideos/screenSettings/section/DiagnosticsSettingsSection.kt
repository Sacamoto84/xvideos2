package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.client.xvideos.common.log.CrashLog
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.common.coil.formatBytes1
import com.client.xvideos.common.share.useCaseShareFile
import com.client.xvideos.common.snackbar.SnackBar

/**
 * Журнал ошибок: посмотреть размер, отдать в «Поделиться», очистить.
 *
 * Журнал никуда не уходит сам — сеть здесь не участвует. Отправить его может
 * только пользователь и только этой кнопкой, поэтому в подписи прямо сказано,
 * что внутри могут быть ссылки.
 */
@Composable
internal fun DiagnosticsSettingsSection() {
    val context = LocalContext.current

    // Пересчитываем размер после очистки и отправки: файл меняется мимо Compose.
    var sizeBytes by remember { mutableLongStateOf(CrashLog.sizeBytes()) }
    val isEmpty = sizeBytes == 0L

    SettingsGroup {
        SettingsValueRow(
            icon = R.drawable.hard_disk_24,
            text = "Журнал ошибок",
            value = if (isEmpty) "Пуст" else formatBytes1(sizeBytes)
        )

        SettingsListItem(
            icon = R.drawable.enter,
            text = "Поделиться журналом",
            subtitle = if (isEmpty) {
                "Пока нечем поделиться"
            } else {
                "Файл может содержать ссылки на контент"
            },
            onClick = {
                if (isEmpty) {
                    SnackBar.info("Журнал пуст")
                } else {
                    useCaseShareFile(context, CrashLog.file)
                }
            }
        )

        SettingsListItem(
            icon = R.drawable.hard_drive_2_24,
            text = "Очистить журнал",
            subtitle = "Удалить записи с устройства",
            onClick = {
                CrashLog.clear()
                sizeBytes = CrashLog.sizeBytes()
                SnackBar.success("Журнал очищен")
            }
        )
    }
}
