package com.client.xvideos.screenSettings.backup

import com.client.xvideos.R
import com.client.xvideos.screenSettings.lDownloadRecoveryConsoleText
import com.client.xvideos.screenSettings.redDownloadRecoveryConsoleText
import com.client.xvideos.screenSettings.shouldAutoRecoverL
import com.client.xvideos.screenSettings.shouldAutoRecoverRedDownload

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupManager
import com.client.xvideos.common.backup.XlrBackupOptions
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsScreenBackground
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.screenSettings.SettingsDataHolders
import kotlinx.coroutines.launch

/**
 * Потолок консоли восстановления.
 *
 * Было 200 строк, и при восстановлении L-мини их съедало за несколько секунд:
 * на каждый файл приходится по две-три записи, а файлов сотни. Начало работы —
 * с которого и понятно, что пошло не так, — вытеснялось раньше, чем его успевали
 * прочитать.
 *
 * Две тысячи коротких строк — это порядка сотни килобайт, и рисуются они
 * `LazyColumn`, то есть только видимые.
 */
private const val BACKUP_CONSOLE_MAX_LINES = 2000

@Composable
internal fun BackupSettingsSection(
    context: Context,
    data: SettingsDataHolders,
    onDataChanged: () -> Unit
) {
    val downloadRed = data.downloadRed
    val savedL = data.savedL
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf(BackupFlowScreen.CREATE) }
    var isWorking by rememberSaveable { mutableStateOf(false) }
    var lBackupMode by rememberSaveable { mutableStateOf(XlrBackupContentMode.MINI) }
    var rBackupMode by rememberSaveable { mutableStateOf(XlrBackupContentMode.MINI) }
    var backupItems by remember { mutableStateOf<List<XlrBackupItem>>(emptyList()) }
    var selectedBackupPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreItems by remember { mutableStateOf<List<XlrBackupItem>>(emptyList()) }
    var selectedRestorePaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val backupConsole = remember { mutableStateListOf<String>() }
    val backupOptions = remember(lBackupMode, rBackupMode) {
        XlrBackupOptions(lMode = lBackupMode, rMode = rBackupMode)
    }

    fun appendBackupLog(message: String) {
        if (backupConsole.size >= BACKUP_CONSOLE_MAX_LINES) {
            backupConsole.removeAt(0)
        }
        backupConsole.add(message)
    }

    suspend fun refreshBackupItems() {
        val items = XlrBackupManager.currentBackupItems(backupOptions)
        backupItems = items
        if (selectedBackupPaths.isEmpty()) {
            selectedBackupPaths = initialSectionSelection(items)
        }
    }

    LaunchedEffect(backupOptions) {
        refreshBackupItems()
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null || isWorking) return@rememberLauncherForActivityResult
        if (selectedBackupPaths.isEmpty()) {
            SnackBar.error("Выберите хотя бы одну папку")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            isWorking = true
            appendBackupLog(
                "Создание backup: L=${backupContentModeTitle(backupOptions.lMode)}, R=${backupContentModeTitle(backupOptions.rMode)}"
            )
            XlrBackupManager.createBackup(context, uri, selectedBackupPaths, backupOptions)
                .onSuccess { report ->
                    appendBackupLog("Backup создан: ${report.files} файлов, ${formatBytes(report.bytes)}")
                    SnackBar.success("Backup создан: ${report.files} файлов, ${formatBytes(report.bytes)}")
                    refreshBackupItems()
                }
                .onFailure { error ->
                    appendBackupLog("Ошибка создания backup: ${error.message ?: error::class.java.simpleName}")
                    SnackBar.error(error.message ?: "Ошибка создания backup")
                }
            isWorking = false
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null || isWorking) return@rememberLauncherForActivityResult
        scope.launch {
            isWorking = true
            XlrBackupManager.inspectBackup(context, uri)
                .onSuccess { items ->
                    restoreUri = uri
                    restoreItems = items
                    selectedRestorePaths = initialSectionSelection(items)
                    SnackBar.success("Backup открыт: ${items.size} папок")
                }
                .onFailure { error ->
                    restoreUri = null
                    restoreItems = emptyList()
                    selectedRestorePaths = emptySet()
                    SnackBar.error(error.message ?: "Ошибка чтения backup")
                }
            isWorking = false
        }
    }

    val backupReport = XlrBackupManager.reportForSelection(backupItems, selectedBackupPaths)
    val restoreReport = XlrBackupManager.reportForSelection(restoreItems, selectedRestorePaths)

    SettingsGroup {
        SettingsValueRow(
            icon = R.drawable.hard_drive_2_24,
            text = "Backup X/L/R",
            value = if (screen == BackupFlowScreen.CREATE) {
                "Создание архива выбранных папок. DB, настройки и кеши не входят в ZIP."
            } else {
                "Восстановление заменяет выбранные папки. Для R Download после restore автоматически проверяются .info."
            }
        )
        BackupModeSelector(
            selected = screen,
            enabled = !isWorking,
            onSelected = { screen = it }
        )
        SettingsDivider()

        when (screen) {
            BackupFlowScreen.CREATE -> {
                SettingsValueRow(
                    icon = R.drawable.hard_drive_2_24,
                    text = "Выбрано для архива",
                    value = if (isWorking) "Идет операция" else selectionSummaryText(backupReport)
                )
                SettingsDivider()
                BackupContentModeSelector(
                    title = "L backup",
                    value = lBackupMode,
                    enabled = !isWorking,
                    description = "Мини: Likes/Collection без медиа, только metadata",
                    onValueChange = { lBackupMode = it }
                )
                SettingsDivider()
                BackupContentModeSelector(
                    title = "R backup",
                    value = rBackupMode,
                    enabled = !isWorking,
                    description = "Мини: Download без mp4/jpg, только .info",
                    onValueChange = { rBackupMode = it }
                )
                SettingsDivider()
                BackupSelectionActions(
                    enabled = !isWorking,
                    onSelectAll = { selectedBackupPaths = initialSectionSelection(backupItems) },
                    onSelectNone = { selectedBackupPaths = emptySet() }
                )
                BackupFolderList(
                    items = backupItems,
                    selectedPaths = selectedBackupPaths,
                    enabled = !isWorking,
                    onToggle = { path -> selectedBackupPaths = toggleBackupPath(backupItems, selectedBackupPaths, path) }
                )
                SettingsDivider()
                SettingsListItem(
                    icon = R.drawable.hard_drive_2_24,
                    text = "Создать ZIP",
                    subtitle = selectionSummaryText(backupReport),
                    trailing = {
                        Button(
                            enabled = !isWorking && selectedBackupPaths.isNotEmpty(),
                            onClick = { createBackupLauncher.launch(XlrBackupManager.defaultFileName()) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SettingsAccentColor,
                                contentColor = SettingsScreenBackground
                            )
                        ) {
                            Text("Создать")
                        }
                    }
                )
            }

            BackupFlowScreen.RESTORE -> {
                SettingsListItem(
                    icon = R.drawable.hard_drive_2_24,
                    text = "Открыть ZIP",
                    subtitle = restoreUri?.lastPathSegment ?: "Сначала выберите архив",
                    trailing = {
                        Button(
                            enabled = !isWorking,
                            onClick = {
                                restoreBackupLauncher.launch(
                                    arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SettingsAccentColor,
                                contentColor = SettingsScreenBackground
                            )
                        ) {
                            Text("Выбрать")
                        }
                    }
                )

                if (restoreItems.isEmpty()) {
                    SettingsDivider()
                    SettingsValueRow(
                        icon = R.drawable.hard_drive_2_24,
                        text = "Что восстановить",
                        value = "Выберите ZIP-файл, после этого появятся папки X, L и R из архива."
                    )
                } else {
                    SettingsDivider()
                    SettingsValueRow(
                        icon = R.drawable.hard_drive_2_24,
                        text = "Выбрано для восстановления",
                        value = if (isWorking) "Идет операция" else selectionSummaryText(restoreReport)
                    )
                    BackupSelectionActions(
                        enabled = !isWorking,
                        onSelectAll = { selectedRestorePaths = initialSectionSelection(restoreItems) },
                        onSelectNone = { selectedRestorePaths = emptySet() }
                    )
                    BackupFolderList(
                        items = restoreItems,
                        selectedPaths = selectedRestorePaths,
                        enabled = !isWorking,
                        onToggle = { path ->
                            selectedRestorePaths = toggleBackupPath(restoreItems, selectedRestorePaths, path)
                        }
                    )
                    SettingsDivider()
                    SettingsButtonRowWithDialog(
                        icon = R.drawable.hard_drive_2_24,
                        text = "Восстановить выбранное",
                        value = if (isWorking) "Идет..." else "Восстановить",
                        textDialogTitle = "Восстановить backup",
                        textDialogBody = "Выбранные папки будут заменены данными из ZIP: ${selectionSummaryText(restoreReport)}. DB, настройки и кеши не трогаются.",
                        textDialogButton = "Восстановить",
                        onClick = {
                            val uri = restoreUri
                            if (uri == null) {
                                SnackBar.error("Сначала выберите ZIP")
                                return@SettingsButtonRowWithDialog
                            }
                            if (selectedRestorePaths.isEmpty()) {
                                SnackBar.error("Выберите хотя бы одну папку")
                                return@SettingsButtonRowWithDialog
                            }
                            if (!isWorking) {
                                scope.launch {
                                    isWorking = true
                                    appendBackupLog("Восстановление backup: ${selectionSummaryText(restoreReport)}")
                                    val autoRecoverL = shouldAutoRecoverL(selectedRestorePaths)
                                    val autoRecoverRedDownload = shouldAutoRecoverRedDownload(selectedRestorePaths)
                                    XlrBackupManager.restoreBackup(context, uri, selectedRestorePaths)
                                        .onSuccess { report ->
                                            refreshBackupItems()
                                            onDataChanged()
                                            // Восстановление меняет файлы мимо приложения, а
                                            // SavedRed и BlockRed — синглтоны со списками в
                                            // памяти: их читают один раз на старте. Без этого
                                            // раздел R оставался пустым до перезапуска, тогда
                                            // как X и L перечитывают свои экраны при входе.
                                            data.savedRed?.refreshAll()
                                            data.blockRed?.refresh()
                                            SnackBar.success("Backup восстановлен: ${report.files} файлов")
                                            appendBackupLog("Backup восстановлен: ${report.files} файлов, ${formatBytes(report.bytes)}")
                                            if (autoRecoverL) {
                                                val lSaved = savedL
                                                if (lSaved == null) {
                                                    SnackBar.error("L Likes/Collection восстановлены, но L-загрузчик недоступен")
                                                } else {
                                                    appendBackupLog("L Likes/Collection: сканирую metadata")
                                                    lSaved.recoverIncompleteSavedMedia(
                                                        onEvent = { message ->
                                                            scope.launch { appendBackupLog(message) }
                                                        },
                                                        onComplete = { recoveryReport ->
                                                            scope.launch { appendBackupLog(lDownloadRecoveryConsoleText(recoveryReport)) }
                                                        }
                                                    )
                                                }
                                            }
                                            if (autoRecoverRedDownload) {
                                                val redDownloader = downloadRed
                                                if (redDownloader == null) {
                                                    SnackBar.error("R Download восстановлен, но загрузчик недоступен")
                                                } else {
                                                    appendBackupLog("R Download: сканирую .info")
                                                    redDownloader.recoverIncompleteDownloads(
                                                        onEvent = { message ->
                                                            scope.launch { appendBackupLog(message) }
                                                        },
                                                        onComplete = { recoveryReport ->
                                                            scope.launch { appendBackupLog(redDownloadRecoveryConsoleText(recoveryReport)) }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        .onFailure { error ->
                                            appendBackupLog("Ошибка восстановления backup: ${error.message ?: error::class.java.simpleName}")
                                            SnackBar.error(error.message ?: "Ошибка восстановления backup")
                                        }
                                    isWorking = false
                                }
                            }
                        }
                    )
                }
            }
        }
        SettingsDivider()
        BackupConsole(
            lines = backupConsole,
            onClear = { backupConsole.clear() }
        )
    }
}
