package com.client.xvideos.common.settings.ui.section

import com.client.xvideos.R
import com.client.xvideos.common.settings.ui.redDownloadRecoveryText

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import com.client.xvideos.common.settings.ui.components.SettingsButtonRowWithDialog
import com.client.xvideos.common.settings.ui.components.SettingsDivider
import com.client.xvideos.common.settings.ui.components.SettingsDividerColor
import com.client.xvideos.common.settings.ui.components.SettingsGroup
import com.client.xvideos.common.settings.ui.components.SettingsListItem
import com.client.xvideos.common.settings.ui.components.SettingsValueRow
import com.client.xvideos.common.settings.ui.components.WhatsAppGreen
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.toPrettyCount3
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.downloader.RedDownloadRecoveryReport
import com.client.xvideos.r.common.saved.SavedRed
import kotlinx.coroutines.launch

@Composable
internal fun RSettingsSection(
    sizeRedTotal: Long,
    sizeRedDownload: Long,
    onClearDownload: () -> Unit,
    savedRed: SavedRed?,
    downloadRed: DownloadRed?,
    isNichesCacheDownloading: Boolean,
    nichesCacheProgress: Float,
    nichesCacheSize: Int,
    nichesCacheLastModifiedHour: Long
) {
    val scope = rememberCoroutineScope()
    var isRecoveringDownload by remember { mutableStateOf(false) }
    var recoveryReport by remember { mutableStateOf<RedDownloadRecoveryReport?>(null) }

    SettingsGroup {
        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Размер всех папок Red",
            value = sizeRedTotal.toPrettyCount3()
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Размер папки Download",
            value = sizeRedDownload.toPrettyCount3()
        )
        SettingsDivider()

        SettingsButtonRowWithDialog(
            icon = R.drawable.icon_red,
            text = "Очистить папку Download",
            value = "Очистить",
            textDialogTitle = "Очистка папки Download",
            textDialogBody = "Подтвердить очистку: ${sizeRedDownload.toPrettyCount3()}",
            textDialogButton = "Очистить",
            onClick = onClearDownload
        )
        SettingsDivider()

        SettingsListItem(
            icon = R.drawable.hard_drive_2_24,
            text = "Докачать Download по .info",
            subtitle = redDownloadRecoveryText(recoveryReport, isRecoveringDownload),
            trailing = {
                Button(
                    enabled = downloadRed != null && !isRecoveringDownload,
                    onClick = {
                        val redDownloader = downloadRed ?: return@Button
                        isRecoveringDownload = true
                        scope.launch {
                            redDownloader.recoverIncompleteDownloads(onComplete = { report ->
                                scope.launch {
                                    recoveryReport = report
                                    isRecoveringDownload = false
                                    if (report.incompleteItems == 0) {
                                        SnackBar.success("Download проверен: все файлы на месте")
                                    } else {
                                        SnackBar.success(
                                            "Запущено: видео ${report.queuedVideo}, превью ${report.queuedPreview}"
                                        )
                                    }
                                }
                            })
                        }
                    }
                ) {
                    Text("Старт")
                }
            }
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Кэш Niches",
            value = "$nichesCacheSize \u2022 ${nichesCacheLastModifiedHour}h"
        )

        if (isNichesCacheDownloading) {
            LinearProgressIndicator(
                progress = { nichesCacheProgress },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            color = WhatsAppGreen,
            trackColor = SettingsDividerColor,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        }
        SettingsDivider()

        SettingsListItem(
            icon = R.drawable.icon_red,
            text = "Обновить кэш Niches",
            subtitle = if (isNichesCacheDownloading) "Идёт обновление" else "Данные для поиска и фильтров R",
            trailing = {
                Button(
                    enabled = savedRed != null && !isNichesCacheDownloading,
                    onClick = { savedRed?.nichesCache?.refresh() }
                ) {
                    Text("Обновить")
                }
            }
        )
    }
}
