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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import com.client.xvideos.common.di.rememberApplicationScope

import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupManager
import com.client.xvideos.common.backup.XlrBackupOptions
import com.client.xvideos.common.backup.XlrBackupType
import com.client.xvideos.common.backup.XlrInvalidPasswordException
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    // ApplicationScope гарантирует, что запись ZIP или распаковка архива не оборвётся
    // посреди файла при переключении страниц настроек или сворачивании (T6).
    val scope = rememberApplicationScope()
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

    // Состояния для парольной защиты бэкапов
    var showCreatePasswordDialog by rememberSaveable { mutableStateOf(false) }
    var createPassword by remember { mutableStateOf<CharArray?>(null) }

    var showRestorePasswordDialog by rememberSaveable { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePassword by remember { mutableStateOf<CharArray?>(null) }
    var restorePasswordError by remember { mutableStateOf<String?>(null) }

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

    DisposableEffect(Unit) {
        onDispose {
            createPassword?.fill('\u0000')
            createPassword = null
            restorePassword?.fill('\u0000')
            restorePassword = null
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val password = createPassword
        if (uri == null || isWorking) {
            password?.fill('\u0000')
            createPassword = null
            return@rememberLauncherForActivityResult
        }
        if (selectedBackupPaths.isEmpty()) {
            password?.fill('\u0000')
            createPassword = null
            SnackBar.error("Выберите хотя бы одну папку")
            return@rememberLauncherForActivityResult
        }
        scope.launch(Dispatchers.Main) {
            isWorking = true
            appendBackupLog(
                "Создание зашифрованного backup: L=${backupContentModeTitle(backupOptions.lMode)}, R=${backupContentModeTitle(backupOptions.rMode)}"
            )
            val result = withContext(Dispatchers.IO) {
                XlrBackupManager.createBackup(context, uri, selectedBackupPaths, backupOptions, password)
            }
            password?.fill('\u0000')
            createPassword = null
            result
                .onSuccess { report ->
                    appendBackupLog("Backup создан и зашифрован: ${report.files} файлов, ${formatBytes(report.bytes)}")
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
        scope.launch(Dispatchers.Main) {
            isWorking = true
            val type = withContext(Dispatchers.IO) {
                XlrBackupManager.detectBackupType(context, uri)
            }
            when (type) {
                XlrBackupType.ENCRYPTED_XLR -> {
                    isWorking = false
                    pendingRestoreUri = uri
                    restorePasswordError = null
                    showRestorePasswordDialog = true
                }
                XlrBackupType.LEGACY_ZIP -> {
                    appendBackupLog("Обнаружен незашифрованный архив (legacy ZIP)")
                    val result = withContext(Dispatchers.IO) {
                        XlrBackupManager.inspectBackup(context, uri, password = null)
                    }
                    result
                        .onSuccess { items ->
                            restoreUri = uri
                            restorePassword?.fill('\u0000')
                            restorePassword = null
                            restoreItems = items
                            selectedRestorePaths = initialSectionSelection(items)
                            SnackBar.success("Backup открыт: ${items.size} папок")
                        }
                        .onFailure { error ->
                            restoreUri = null
                            restorePassword?.fill('\u0000')
                            restorePassword = null
                            restoreItems = emptyList()
                            selectedRestorePaths = emptySet()
                            SnackBar.error(error.message ?: "Ошибка чтения backup")
                        }
                    isWorking = false
                }
                XlrBackupType.UNSUPPORTED -> {
                    isWorking = false
                    restoreUri = null
                    restorePassword?.fill('\u0000')
                    restorePassword = null
                    restoreItems = emptyList()
                    selectedRestorePaths = emptySet()
                    SnackBar.error("Неподдерживаемый формат файла: не является бэкапом XLR или ZIP")
                }
            }
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
                    text = "Создать бэкап",
                    subtitle = selectionSummaryText(backupReport),
                    trailing = {
                        Button(
                            enabled = !isWorking && selectedBackupPaths.isNotEmpty(),
                            onClick = { showCreatePasswordDialog = true },
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
                    text = "Открыть архив",
                    subtitle = restoreUri?.lastPathSegment ?: "Сначала выберите архив",
                    trailing = {
                        Button(
                            enabled = !isWorking,
                            onClick = {
                                restoreBackupLauncher.launch(
                                    arrayOf("application/octet-stream", "application/zip", "application/x-zip-compressed", "*/*")
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
                        value = "Выберите файл бэкапа (.xlr или .zip), после этого появятся папки X, L и R из архива."
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
                        textDialogBody = "Выбранные папки будут заменены данными из архива: ${selectionSummaryText(restoreReport)}. DB, настройки и кеши не трогаются.",
                        textDialogButton = "Восстановить",
                        onClick = {
                            val uri = restoreUri
                            if (uri == null) {
                                SnackBar.error("Сначала выберите архив")
                                return@SettingsButtonRowWithDialog
                            }
                            if (selectedRestorePaths.isEmpty()) {
                                SnackBar.error("Выберите хотя бы одну папку")
                                return@SettingsButtonRowWithDialog
                            }
                            if (!isWorking) {
                                scope.launch(Dispatchers.Main) {
                                    isWorking = true
                                    appendBackupLog("Восстановление backup: ${selectionSummaryText(restoreReport)}")
                                    val autoRecoverL = shouldAutoRecoverL(selectedRestorePaths)
                                    val autoRecoverRedDownload = shouldAutoRecoverRedDownload(selectedRestorePaths)
                                    val result = withContext(Dispatchers.IO) {
                                        XlrBackupManager.restoreBackup(context, uri, selectedRestorePaths, restorePassword)
                                    }
                                    restorePassword?.fill('\u0000')
                                    restorePassword = null
                                    result
                                        .onSuccess { report ->
                                            refreshBackupItems()
                                            onDataChanged()
                                            // Восстановление меняет файлы мимо приложения, а
                                            // SavedRed и BlockRed — синглтоны со списками в
                                            // памяти: их читают один раз на старте. Без этого
                                            // раздел R оставался пустым до перезапуска, тогда
                                            // как X и L перечитывают свои экраны при входе.
                                            withContext(Dispatchers.IO) {
                                                data.savedRed?.refreshAll()
                                                data.blockRed?.refresh()
                                                data.downloadRed?.refreshDownloadList()
                                            }
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
                                                            scope.launch(Dispatchers.Main) { appendBackupLog(message) }
                                                        },
                                                        onComplete = { recoveryReport ->
                                                            scope.launch(Dispatchers.Main) { appendBackupLog(lDownloadRecoveryConsoleText(recoveryReport)) }
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
                                                            scope.launch(Dispatchers.Main) { appendBackupLog(message) }
                                                        },
                                                        onComplete = { recoveryReport ->
                                                            scope.launch(Dispatchers.Main) { appendBackupLog(redDownloadRecoveryConsoleText(recoveryReport)) }
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

    if (showCreatePasswordDialog) {
        BackupCreatePasswordDialog(
            onDismiss = {
                showCreatePasswordDialog = false
            },
            onConfirm = { password ->
                showCreatePasswordDialog = false
                createPassword = password
                createBackupLauncher.launch(XlrBackupManager.defaultFileName())
            }
        )
    }

    if (showRestorePasswordDialog) {
        BackupRestorePasswordDialog(
            errorMessage = restorePasswordError,
            onDismiss = {
                showRestorePasswordDialog = false
                pendingRestoreUri = null
                restorePasswordError = null
            },
            onConfirm = { password ->
                val uri = pendingRestoreUri ?: run {
                    password.fill('\u0000')
                    return@BackupRestorePasswordDialog
                }
                scope.launch(Dispatchers.Main) {
                    isWorking = true
                    val result = withContext(Dispatchers.IO) {
                        XlrBackupManager.inspectBackup(context, uri, password)
                    }
                    result
                        .onSuccess { items ->
                            showRestorePasswordDialog = false
                            restorePasswordError = null
                            restoreUri = uri
                            restorePassword?.fill('\u0000')
                            restorePassword = password
                            restoreItems = items
                            selectedRestorePaths = initialSectionSelection(items)
                            SnackBar.success("Архив успешно расшифрован: ${items.size} папок")
                        }
                        .onFailure { error ->
                            password.fill('\u0000')
                            val message = if (error is XlrInvalidPasswordException || error.cause is XlrInvalidPasswordException) {
                                "Неверный пароль для расшифровки бэкапа"
                            } else {
                                error.message ?: "Ошибка расшифровки бэкапа"
                            }
                            restorePasswordError = message
                            SnackBar.error(message)
                        }
                    isWorking = false
                }
            }
        )
    }
}
