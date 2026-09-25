package com.client.xvideos.screenSettings.backup

import com.client.xvideos.R
import com.client.xvideos.screenSettings.lDownloadRecoveryConsoleText
import com.client.xvideos.screenSettings.redDownloadRecoveryConsoleText
import com.client.xvideos.screenSettings.shouldAutoRecoverL
import com.client.xvideos.screenSettings.shouldAutoRecoverRedDownload

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsScreenBackground
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.screenSettings.SettingsDataHolders
import com.client.xvideos.screenSettings.components.SettingsDivider2
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
private const val BACKUP_HEADER_CREATE = "Создание архива выбранных папок. DB, настройки и кеши не входят в ZIP."
private const val BACKUP_HEADER_RESTORE = "Восстановление заменяет выбранные папки. Для R Download после restore автоматически проверяются .info."
private const val BACKUP_OPERATION_IN_PROGRESS = "Идет операция"
private const val RESTORE_SUBTITLE_PLACEHOLDER = "Сначала выберите архив"
private const val MSG_WAIT_OPERATION = "Пожалуйста, дождитесь окончания операции"
private const val MSG_SELECT_AT_LEAST_ONE_FOLDER = "Выберите хотя бы одну папку"
private const val MSG_UNSUPPORTED_BACKUP_FORMAT = "Неподдерживаемый формат файла: не является бэкапом XLR или ZIP"
private const val MSG_RESTORE_FIRST_SELECT_ARCHIVE = "Сначала выберите архив"
private const val RESTORE_HELP_EMPTY_TEXT = "Выберите файл бэкапа (.xlr или .zip), после этого появятся папки X, L и R из архива."
private const val INVALID_PASSWORD_ERROR_TEXT = "Неверный пароль для расшифровки бэкапа"

private val RESTORE_MIME_TYPES = arrayOf(
    "application/octet-stream",
    "application/zip",
    "application/x-zip-compressed",
    "*/*"
)

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun BackupSettingsSection(
    context: Context,
    data: SettingsDataHolders,
    onDataChanged: () -> Unit,
    modifier: Modifier = Modifier
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
    var restoreUri by rememberSaveable { mutableStateOf<Uri?>(null) }
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
    var pendingRestoreUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var restorePassword by remember { mutableStateOf<CharArray?>(null) }
    var restorePasswordError by rememberSaveable { mutableStateOf<String?>(null) }

    val onBack = remember(isWorking, showCreatePasswordDialog, showRestorePasswordDialog, screen) {
        {
            when {
                isWorking -> SnackBar.info(MSG_WAIT_OPERATION)
                showCreatePasswordDialog -> showCreatePasswordDialog = false
                showRestorePasswordDialog -> {
                    showRestorePasswordDialog = false
                    pendingRestoreUri = null
                    restorePasswordError = null
                }
                screen == BackupFlowScreen.RESTORE -> screen = BackupFlowScreen.CREATE
            }
        }
    }

    BackHandler(
        enabled = isWorking || showCreatePasswordDialog || showRestorePasswordDialog || screen == BackupFlowScreen.RESTORE,
        onBack = onBack
    )

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
            SnackBar.error(MSG_SELECT_AT_LEAST_ONE_FOLDER)
            return@rememberLauncherForActivityResult
        }
        scope.launch(Dispatchers.Main) {
            isWorking = true
            try {
                appendBackupLog(
                    "Создание зашифрованного backup: L=${backupContentModeTitle(backupOptions.lMode)}, R=${backupContentModeTitle(backupOptions.rMode)}"
                )
                val result = withContext(Dispatchers.IO) {
                    XlrBackupManager.createBackup(context, uri, selectedBackupPaths, backupOptions, password)
                }
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
            } finally {
                password?.fill('\u0000')
                createPassword = null
                isWorking = false
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null || isWorking) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.Main) {
            isWorking = true
            try {
                val type = withContext(Dispatchers.IO) {
                    XlrBackupManager.detectBackupType(context, uri)
                }
                when (type) {
                    XlrBackupType.ENCRYPTED_XLR -> {
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
                    }
                    XlrBackupType.UNSUPPORTED -> {
                        restoreUri = null
                        restorePassword?.fill('\u0000')
                        restorePassword = null
                        restoreItems = emptyList()
                        selectedRestorePaths = emptySet()
                        SnackBar.error(MSG_UNSUPPORTED_BACKUP_FORMAT)
                    }
                }
            } finally {
                isWorking = false
            }
        }
    }

    val backupReport = remember(backupItems, selectedBackupPaths) {
        XlrBackupManager.reportForSelection(backupItems, selectedBackupPaths)
    }
    val restoreReport = remember(restoreItems, selectedRestorePaths) {
        XlrBackupManager.reportForSelection(restoreItems, selectedRestorePaths)
    }

    val onSelectScreen = remember { { target: BackupFlowScreen -> screen = target } }
    val onLBackupModeChange = remember { { mode: XlrBackupContentMode -> lBackupMode = mode } }
    val onRBackupModeChange = remember { { mode: XlrBackupContentMode -> rBackupMode = mode } }
    val onSelectAllBackup = remember(backupItems) {
        { selectedBackupPaths = initialSectionSelection(backupItems) }
    }
    val onSelectNoneBackup = remember { { selectedBackupPaths = emptySet() } }
    val onToggleBackupPath = remember(backupItems, selectedBackupPaths) {
        { path: String -> selectedBackupPaths = toggleBackupPath(backupItems, selectedBackupPaths, path) }
    }
    val onShowCreatePasswordDialog = remember { { showCreatePasswordDialog = true } }
    val onSelectAllRestore = remember(restoreItems) {
        { selectedRestorePaths = initialSectionSelection(restoreItems) }
    }
    val onSelectNoneRestore = remember { { selectedRestorePaths = emptySet() } }
    val onToggleRestorePath = remember(restoreItems, selectedRestorePaths) {
        { path: String -> selectedRestorePaths = toggleBackupPath(restoreItems, selectedRestorePaths, path) }
    }
    val onClearConsole = remember { { backupConsole.clear() } }
    val onDismissCreatePasswordDialog = remember { { showCreatePasswordDialog = false } }
    val onDismissRestorePasswordDialog = remember {
        {
            showRestorePasswordDialog = false
            pendingRestoreUri = null
            restorePasswordError = null
        }
    }

    val backupHeaderValue = remember(screen) {
        if (screen == BackupFlowScreen.CREATE) {
            BACKUP_HEADER_CREATE
        } else {
            BACKUP_HEADER_RESTORE
        }
    }
    val backupSummaryText = remember(backupReport) { selectionSummaryText(backupReport) }
    val backupValueText = remember(isWorking, backupSummaryText) {
        if (isWorking) BACKUP_OPERATION_IN_PROGRESS else backupSummaryText
    }
    val restoreSubtitle = remember(restoreUri) {
        restoreUri?.lastPathSegment ?: RESTORE_SUBTITLE_PLACEHOLDER
    }
    val restoreSummaryText = remember(restoreReport) { selectionSummaryText(restoreReport) }
    val restoreValueText = remember(isWorking, restoreSummaryText) {
        if (isWorking) BACKUP_OPERATION_IN_PROGRESS else restoreSummaryText
    }
    val restoreDialogBody = remember(restoreSummaryText) {
        "Выбранные папки будут заменены данными из архива: $restoreSummaryText. DB, настройки и кеши не трогаются."
    }

    val actionButtonColors = ButtonDefaults.buttonColors(
        containerColor = SettingsAccentColor,
        contentColor = SettingsScreenBackground
    )

    SettingsGroup(modifier = modifier) {
        SettingsValueRow(
            icon = R.drawable.hard_drive_2_24,
            text = "Backup X/L/R",
            value = backupHeaderValue
        )
        BackupModeSelector(
            selected = screen,
            enabled = !isWorking,
            onSelected = onSelectScreen
        )
        SettingsDivider()

        when (screen) {
            BackupFlowScreen.CREATE -> {
                SettingsDivider2()
                SettingsValueRow(
                    icon = R.drawable.hard_drive_2_24,
                    text = "Выбрано для архива",
                    value = backupValueText
                )
                SettingsDivider2()

                BackupContentModeSelector(
                    title = "L backup",
                    value = lBackupMode,
                    enabled = !isWorking,
                    description = "Мини: Likes/Collection без медиа, только metadata",
                    onValueChange = onLBackupModeChange
                )

                SettingsDivider2()

                BackupContentModeSelector(
                    title = "R backup",
                    value = rBackupMode,
                    enabled = !isWorking,
                    description = "Мини: Download без mp4/jpg, только .info",
                    onValueChange = onRBackupModeChange
                )

                SettingsDivider2()

                BackupSelectionActions(
                    enabled = !isWorking,
                    onSelectAll = onSelectAllBackup,
                    onSelectNone = onSelectNoneBackup
                )
                BackupFolderList(
                    items = backupItems,
                    selectedPaths = selectedBackupPaths,
                    enabled = !isWorking,
                    onToggle = onToggleBackupPath
                )
                SettingsDivider()
                SettingsListItem(
                    icon = R.drawable.hard_drive_2_24,
                    text = "Создать бэкап",
                    subtitle = backupSummaryText,
                    trailing = {
                        Button(
                            enabled = !isWorking && selectedBackupPaths.isNotEmpty(),
                            onClick = onShowCreatePasswordDialog,
                            colors = actionButtonColors
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
                    subtitle = restoreSubtitle,
                    trailing = {
                        Button(
                            enabled = !isWorking,
                            onClick = {
                                restoreBackupLauncher.launch(RESTORE_MIME_TYPES)
                            },
                            colors = actionButtonColors
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
                        value = RESTORE_HELP_EMPTY_TEXT
                    )
                } else {
                    SettingsDivider()
                    SettingsValueRow(
                        icon = R.drawable.hard_drive_2_24,
                        text = "Выбрано для восстановления",
                        value = restoreValueText
                    )
                    BackupSelectionActions(
                        enabled = !isWorking,
                        onSelectAll = onSelectAllRestore,
                        onSelectNone = onSelectNoneRestore
                    )
                    BackupFolderList(
                        items = restoreItems,
                        selectedPaths = selectedRestorePaths,
                        enabled = !isWorking,
                        onToggle = onToggleRestorePath
                    )
                    SettingsDivider()
                    SettingsButtonRowWithDialog(
                        icon = R.drawable.hard_drive_2_24,
                        text = "Восстановить выбранное",
                        value = if (isWorking) "Идет..." else "Восстановить",
                        textDialogTitle = "Восстановить backup",
                        textDialogBody = restoreDialogBody,
                        textDialogButton = "Восстановить",
                        onClick = {
                            val uri = restoreUri
                            if (uri == null) {
                                SnackBar.error(MSG_RESTORE_FIRST_SELECT_ARCHIVE)
                                return@SettingsButtonRowWithDialog
                            }
                            if (selectedRestorePaths.isEmpty()) {
                                SnackBar.error(MSG_SELECT_AT_LEAST_ONE_FOLDER)
                                return@SettingsButtonRowWithDialog
                            }
                            if (!isWorking) {
                                scope.launch(Dispatchers.Main) {
                                    isWorking = true
                                    try {
                                        appendBackupLog("Восстановление backup: $restoreSummaryText")
                                        val autoRecoverL = shouldAutoRecoverL(selectedRestorePaths)
                                        val autoRecoverRedDownload = shouldAutoRecoverRedDownload(selectedRestorePaths)
                                        val result = withContext(Dispatchers.IO) {
                                            XlrBackupManager.restoreBackup(context, uri, selectedRestorePaths, restorePassword)
                                        }
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
                                    } finally {
                                        restorePassword?.fill('\u0000')
                                        restorePassword = null
                                        isWorking = false
                                    }
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
            onClear = onClearConsole
        )
    }

    if (showCreatePasswordDialog) {
        BackupCreatePasswordDialog(
            onDismiss = onDismissCreatePasswordDialog,
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
            onDismiss = onDismissRestorePasswordDialog,
            onConfirm = { password ->
                val uri = pendingRestoreUri ?: run {
                    password.fill('\u0000')
                    return@BackupRestorePasswordDialog
                }
                scope.launch(Dispatchers.Main) {
                    isWorking = true
                    try {
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
                                    INVALID_PASSWORD_ERROR_TEXT
                                } else {
                                    error.message ?: "Ошибка расшифровки бэкапа"
                                }
                                restorePasswordError = message
                                SnackBar.error(message)
                            }
                    } finally {
                        isWorking = false
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun BackupSettingsSectionPreview() = SettingsPreview {
    val context = LocalContext.current
    BackupSettingsSection(
        context = context,
        data = SettingsDataHolders(),
        onDataChanged = {}
    )
}
