package com.client.xvideos.screenSettings.section

import com.client.xvideos.R
import com.client.xvideos.screenSettings.redDownloadRecoveryText

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

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsDividerColor
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.screenSettings.components.WhatsAppGreen
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.downloader.RedDownloadRecoveryReport
import com.client.xvideos.r.common.saved.SavedRed
import kotlinx.coroutines.launch

private val PROGRESS_HORIZONTAL_PADDING = 16.dp

private const val TEXT_RED_ALL_FOLDERS = "Размер всех папок Red"
private const val TEXT_RED_DOWNLOAD_FOLDER = "Размер папки Download"
private const val TEXT_CLEAR_DOWNLOAD = "Очистить папку Download"
private const val TEXT_CLEAR = "Очистить"
private const val TEXT_CLEAR_DIALOG_TITLE = "Очистка папки Download"
private const val TEXT_RECOVER_DOWNLOAD = "Докачать Download по .info"
private const val TEXT_NICHES_CACHE = "Кэш Niches"
private const val TEXT_UPDATE_NICHES_CACHE = "Обновить кэш Niches"
private const val TEXT_START = "Старт"
private const val TEXT_UPDATE = "Обновить"
private const val TEXT_UPDATING = "Идёт обновление"
private const val TEXT_NICHES_SUBTITLE_DEFAULT = "Данные для поиска и фильтров R"
private const val CLEAR_DIALOG_BODY_PREFIX = "Подтвердить очистку: "
private const val SNACK_DOWNLOAD_CHECKED_OK = "Download проверен: все файлы на месте"
private const val SNACK_RECOVERY_STARTED_PREFIX = "Запущено: видео "
private const val SNACK_RECOVERY_PREVIEW_PREFIX = ", превью "
private const val NICHES_CACHE_SEPARATOR = " \u2022 "
private const val NICHES_CACHE_HOUR_SUFFIX = "h"
private const val ICON_RED = R.drawable.icon_red
private const val ICON_HARD_DRIVE = R.drawable.hard_drive_2_24

private val PROGRESS_INDICATOR_BASE_MODIFIER = Modifier
    .padding(horizontal = PROGRESS_HORIZONTAL_PADDING)
    .fillMaxWidth()

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
                    redDownloader.recoverIncompleteDownloads(onComplete = { report ->
                        scope.launch {
                            recoveryReport = report
                            isRecoveringDownload = false
                            if (report.incompleteItems == 0) {
                                SnackBar.success(SNACK_DOWNLOAD_CHECKED_OK)
                            } else {
                                SnackBar.success(
                                    "$SNACK_RECOVERY_STARTED_PREFIX${report.queuedVideo}$SNACK_RECOVERY_PREVIEW_PREFIX${report.queuedPreview}"
                                )
                            }
                        }
                    })
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
            RecoveryTrailingButton(enabled = isRecoveryEnabled, onClick = onStartRecovery)
        }
    }

    val nichesTrailing: @Composable () -> Unit = remember(isNichesEnabled, onRefreshNichesCache) {
        {
            NichesTrailingButton(enabled = isNichesEnabled, onClick = onRefreshNichesCache)
        }
    }

    val formattedTotal = remember(sizeRedTotal) { formatBytes(sizeRedTotal) }
    val formattedDownload = remember(sizeRedDownload) { formatBytes(sizeRedDownload) }
    val clearDialogBody = remember(formattedDownload) { "$CLEAR_DIALOG_BODY_PREFIX$formattedDownload" }
    val nichesCacheValue = remember(nichesCacheSize, nichesCacheLastModifiedHour) {
        "$nichesCacheSize$NICHES_CACHE_SEPARATOR$nichesCacheLastModifiedHour$NICHES_CACHE_HOUR_SUFFIX"
    }
    val nichesSubtitle = if (isNichesCacheDownloading) TEXT_UPDATING else TEXT_NICHES_SUBTITLE_DEFAULT
    val recoverySubtitle = remember(recoveryReport, isRecoveringDownload) {
        redDownloadRecoveryText(recoveryReport, isRecoveringDownload)
    }

    SettingsGroup(modifier = modifier) {
        SettingsValueRow(
            icon = ICON_RED,
            text = TEXT_RED_ALL_FOLDERS,
            value = formattedTotal
        )
        SettingsDivider()

        SettingsValueRow(
            icon = ICON_RED,
            text = TEXT_RED_DOWNLOAD_FOLDER,
            value = formattedDownload
        )
        SettingsDivider()

        SettingsButtonRowWithDialog(
            icon = ICON_RED,
            text = TEXT_CLEAR_DOWNLOAD,
            value = TEXT_CLEAR,
            textDialogTitle = TEXT_CLEAR_DIALOG_TITLE,
            textDialogBody = clearDialogBody,
            textDialogButton = TEXT_CLEAR,
            onClick = onClearDownload
        )
        SettingsDivider()

        SettingsListItem(
            icon = ICON_HARD_DRIVE,
            text = TEXT_RECOVER_DOWNLOAD,
            subtitle = recoverySubtitle,
            trailing = recoveryTrailing
        )
        SettingsDivider()

        SettingsValueRow(
            icon = ICON_RED,
            text = TEXT_NICHES_CACHE,
            value = nichesCacheValue
        )

        if (isNichesCacheDownloading) {
            val progressProvider = remember(nichesCacheProgress) { { nichesCacheProgress } }
            LinearProgressIndicator(
                progress = progressProvider,
                modifier = PROGRESS_INDICATOR_BASE_MODIFIER,
                color = WhatsAppGreen,
                trackColor = SettingsDividerColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
        }
        SettingsDivider()

        SettingsListItem(
            icon = ICON_RED,
            text = TEXT_UPDATE_NICHES_CACHE,
            subtitle = nichesSubtitle,
            trailing = nichesTrailing
        )
    }
}

@Composable
private fun RecoveryTrailingButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(TEXT_START)
    }
}

@Composable
private fun NichesTrailingButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(TEXT_UPDATE)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun RSettingsSectionPreview() = SettingsPreview {
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
