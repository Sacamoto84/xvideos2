package com.client.xvideos.screenSettings.section

import com.client.xvideos.R
import com.client.xvideos.screenSettings.redDownloadRecoveryText

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.screenSettings.atom.SettingsActionButton
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsDividerColor
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.screenSettings.components.WhatsAppGreen
import com.client.xvideos.screenSettings.molecule.RProfileRow
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.common.util.formatBytes
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
    nichesCacheLastModifiedHour: Long,
    modifier: Modifier = Modifier,
    onOpenLogin: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var isRecoveringDownload by remember { mutableStateOf(false) }
    var recoveryReport by remember { mutableStateOf<RedDownloadRecoveryReport?>(null) }

    val onStartRecovery = remember(downloadRed, scope) {
        {
            val redDownloader = downloadRed
            if (redDownloader != null) {
                isRecoveringDownload = true
                scope.launch {
                    try {
                        runCatchingCancellable { redDownloader.recoverIncompleteDownloads() }
                            .onSuccess { report ->
                                recoveryReport = report
                                val message = if (report.incompleteItems == 0) {
                                    "Download проверен: все файлы на месте"
                                } else {
                                    "Докачка завершена. Поставлено в очередь: видео ${report.queuedVideo}, превью ${report.queuedPreview}"
                                }
                                SnackBar.info(message)
                            }
                            .onFailure { SnackBar.error("Не удалось завершить докачку R") }
                    } finally {
                        isRecoveringDownload = false
                    }
                }
            }
        }
    }

    val onRefreshNichesCache: () -> Unit = remember(savedRed) {
        { savedRed?.nichesCache?.refresh() }
    }

    val isRecoveryEnabled = downloadRed != null && !isRecoveringDownload
    val isNichesEnabled = savedRed != null && !isNichesCacheDownloading

    val recoveryTrailing: @Composable () -> Unit = remember(isRecoveryEnabled, onStartRecovery) {
        {
            SettingsActionButton(text = "Старт", enabled = isRecoveryEnabled, onClick = onStartRecovery)
        }
    }

    val nichesTrailing: @Composable () -> Unit = remember(isNichesEnabled, onRefreshNichesCache) {
        {
            SettingsActionButton(text = "Обновить", enabled = isNichesEnabled, onClick = onRefreshNichesCache)
        }
    }

    val formattedTotal = remember(sizeRedTotal) { formatBytes(sizeRedTotal) }
    val formattedDownload = remember(sizeRedDownload) { formatBytes(sizeRedDownload) }
    val clearDialogBody = remember(formattedDownload) { "Подтвердить очистку: $formattedDownload" }
    val nichesCacheValue = remember(nichesCacheSize, nichesCacheLastModifiedHour) {
        "$nichesCacheSize \u2022 ${nichesCacheLastModifiedHour}h"
    }
    val nichesSubtitle = if (isNichesCacheDownloading) "Идёт обновление" else "Данные для поиска и фильтров R"
    val recoverySubtitle = remember(recoveryReport, isRecoveringDownload) {
        redDownloadRecoveryText(recoveryReport, isRecoveringDownload)
    }

    SettingsGroup(modifier = modifier) {
        RProfileRow(onLogin = onOpenLogin)
        SettingsDivider()
        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Размер всех папок Red",
            value = formattedTotal
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Размер папки Download",
            value = formattedDownload
        )
        SettingsDivider()

        SettingsButtonRowWithDialog(
            icon = R.drawable.icon_red,
            text = "Очистить папку Download",
            value = "Очистить",
            textDialogTitle = "Очистка папки Download",
            textDialogBody = clearDialogBody,
            textDialogButton = "Очистить",
            onClick = onClearDownload
        )
        SettingsDivider()

        SettingsListItem(
            icon = R.drawable.hard_drive_2_24,
            text = "Докачать Download по .info",
            subtitle = recoverySubtitle,
            trailing = recoveryTrailing
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.icon_red,
            text = "Кэш Niches",
            value = nichesCacheValue
        )

        if (isNichesCacheDownloading) {
            val progressProvider = remember(nichesCacheProgress) { { nichesCacheProgress } }
            LinearProgressIndicator(
                progress = progressProvider,
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
            subtitle = nichesSubtitle,
            trailing = nichesTrailing
        )
    }
}

@Preview
@Composable
private fun RSettingsSectionPreview() {
    RSettingsSection(
        sizeRedTotal = 1024L * 1024L * 250L,
        sizeRedDownload = 1024L * 1024L * 50L,
        onClearDownload = {},
        savedRed = null,
        downloadRed = null,
        isNichesCacheDownloading = false,
        nichesCacheProgress = 0.6f,
        nichesCacheSize = 142,
        nichesCacheLastModifiedHour = 3L
    )
}
