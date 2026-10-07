package com.client.xvideos.screenSettings.backup

import com.client.xvideos.R

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue


import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.common.backup.XlrBackupReport
import com.client.xvideos.common.backup.XlrBackupManager
import com.client.xvideos.common.backup.XlrBackupOptions
import com.client.xvideos.common.backup.XlrBackupType
import com.client.xvideos.common.backup.XlrRestoreMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsScreenBackground
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.screenSettings.components.SettingsDivider2
import com.client.xvideos.screenSettings.molecule.SettingsButtonRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val RESTORE_MIME_TYPES = arrayOf(
    "application/octet-stream",
    "application/zip",
    "application/x-zip-compressed",
    "*/*"
)

/**
 * Страница бэкапа. Только рисует: состояние и операции держит [BackupController],
 * который живёт вне композиции — см. его описание. Здесь остаётся лишь то, что
 * принадлежит экрану: открытая вкладка и видимость диалогов пароля и режима
 * восстановления.
 *
 * @param controller держатель бэкапа; `null` — превью без DI.
 * @param onDataChanged файлы на диске изменились: пересчитать статистику.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun BackupSettingsSection(
    controller: BackupController?,
    onDataChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewScope = rememberCoroutineScope()
    val backup = controller ?: remember { BackupController(PreviewBackupEngine, previewScope, Dispatchers.Main) }

    var screen by rememberSaveable { mutableStateOf(BackupFlowScreen.CREATE) }
    var showCreatePasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreModeDialog by rememberSaveable { mutableStateOf(false) }

    val isWorking = backup.isWorking
    val isIdle = backup.isIdle
    val backupItems = backup.backupItems
    val selectedBackupPaths = backup.selectedBackupPaths
    val restoreItems = backup.restoreItems
    val selectedRestorePaths = backup.selectedRestorePaths
    val restoreUri = backup.restoreUri
    val showRestorePasswordDialog = backup.showRestorePasswordDialog

    val onBack = remember(backup, isWorking, showCreatePasswordDialog, showRestorePasswordDialog, screen) {
        {
            when {
                isWorking -> SnackBar.info("Пожалуйста, дождитесь окончания операции")
                showCreatePasswordDialog -> showCreatePasswordDialog = false
                showRestorePasswordDialog -> backup.dismissRestorePassword()
                screen == BackupFlowScreen.RESTORE -> screen = BackupFlowScreen.CREATE
            }
        }
    }

    BackHandler(
        enabled = isWorking || showCreatePasswordDialog || showRestorePasswordDialog || screen == BackupFlowScreen.RESTORE,
        onBack = onBack
    )

    LaunchedEffect(backup, backup.lMode, backup.rMode) {
        backup.refreshBackupItems()
    }

    // Восстановление изменило файлы на диске: экран пересчитывает статистику.
    var seenRestoreCount by remember(backup) { mutableIntStateOf(backup.restoreCount) }
    LaunchedEffect(backup.restoreCount) {
        if (backup.restoreCount != seenRestoreCount) {
            seenRestoreCount = backup.restoreCount
            onDataChanged()
        }
    }

    // Результат выбора файла уходит держателю: пароль лежит у него и не
    // зависит от того, пересоздавался ли экран, пока был открыт системный диалог.
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> backup.createBackup(uri?.toString()) }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> backup.openArchive(uri?.toString()) }

    val backupReport = remember(backupItems, selectedBackupPaths) {
        XlrBackupManager.reportForSelection(backupItems, selectedBackupPaths)
    }
    val restoreReport = remember(restoreItems, selectedRestorePaths) {
        XlrBackupManager.reportForSelection(restoreItems, selectedRestorePaths)
    }

    val onSelectScreen = remember { { target: BackupFlowScreen -> screen = target } }
    val onLBackupModeChange = remember(backup) { { mode: XlrBackupContentMode -> backup.lMode = mode } }
    val onRBackupModeChange = remember(backup) { { mode: XlrBackupContentMode -> backup.rMode = mode } }
    val onSelectAllBackup = remember(backup) { { backup.selectAllBackup() } }
    val onSelectNoneBackup = remember(backup) { { backup.selectNoneBackup() } }
    val onToggleBackupPath = remember(backup) { { path: String -> backup.toggleBackupPath(path) } }
    val onShowCreatePasswordDialog = remember { { showCreatePasswordDialog = true } }
    val onSelectAllRestore = remember(backup) { { backup.selectAllRestore() } }
    val onSelectNoneRestore = remember(backup) { { backup.selectNoneRestore() } }
    val onToggleRestorePath = remember(backup) { { path: String -> backup.toggleRestorePath(path) } }
    val onClearConsole = remember(backup) { { backup.clearConsole() } }
    val onDismissCreatePasswordDialog = remember { { showCreatePasswordDialog = false } }
    val onDismissRestorePasswordDialog = remember(backup) { { backup.dismissRestorePassword() } }
    // Вопрос о режиме задаётся, только когда восстановление может начаться:
    // иначе пользователь выбирал бы режим ради сообщения «выберите папку».
    val onAskRestoreMode = remember(backup) {
        {
            when {
                !backup.isIdle -> Unit
                backup.selectedRestorePaths.isEmpty() -> SnackBar.error(MSG_SELECT_AT_LEAST_ONE_FOLDER)
                else -> showRestoreModeDialog = true
            }
        }
    }
    val onDismissRestoreModeDialog = remember { { showRestoreModeDialog = false } }
    val onRestore = remember(backup) {
        { mode: XlrRestoreMode ->
            showRestoreModeDialog = false
            backup.restore(mode)
        }
    }

    val backupHeaderValue = remember(screen) {
        if (screen == BackupFlowScreen.CREATE) {
            "Создание архива выбранных папок. DB, настройки и кеши не входят в ZIP."
        } else {
            "Восстановление заменяет выбранные папки или добавляет к ним содержимое архива — режим " +
                "спрашивается перед запуском. Для R Download после restore автоматически проверяются .info."
        }
    }
    val backupSummaryText = remember(backupReport) { selectionSummaryText(backupReport) }
    val busyText = when {
        isWorking -> "Идет операция"
        backup.isRecovering -> "Идет докачка"
        else -> null
    }
    val backupValueText = busyText ?: backupSummaryText
    val restoreSubtitle = remember(restoreUri) {
        restoreUri?.let { Uri.parse(it).lastPathSegment } ?: "Сначала выберите архив"
    }
    val restoreSummaryText = remember(restoreReport) { selectionSummaryText(restoreReport) }
    val restoreValueText = busyText ?: restoreSummaryText

    val actionButtonColors = ButtonDefaults.buttonColors(
        containerColor = SettingsAccentColor,
        contentColor = SettingsScreenBackground
    )

    val isCreateEnabled = isIdle && selectedBackupPaths.isNotEmpty()
    val createButtonTrailing: @Composable () -> Unit = remember(isCreateEnabled, onShowCreatePasswordDialog) {
        {
            Button(
                enabled = isCreateEnabled,
                onClick = onShowCreatePasswordDialog,
                colors = actionButtonColors
            ) {
                Text("Создать")
            }
        }
    }

    val onLaunchRestoreBackup = remember(restoreBackupLauncher) {
        { restoreBackupLauncher.launch(RESTORE_MIME_TYPES) }
    }
    val restoreButtonTrailing: @Composable () -> Unit = remember(isIdle, onLaunchRestoreBackup) {
        {
            Button(
                enabled = isIdle,
                onClick = onLaunchRestoreBackup,
                colors = actionButtonColors
            ) {
                Text("Выбрать")
            }
        }
    }

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
                    value = backup.lMode,
                    enabled = !isWorking,
                    description = "Мини: Likes/Collection без медиа, только metadata",
                    onValueChange = onLBackupModeChange
                )

                SettingsDivider2()

                BackupContentModeSelector(
                    title = "R backup",
                    value = backup.rMode,
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
                    trailing = createButtonTrailing
                )
            }

            BackupFlowScreen.RESTORE -> {
                SettingsListItem(
                    icon = R.drawable.hard_drive_2_24,
                    text = "Открыть архив",
                    subtitle = restoreSubtitle,
                    trailing = restoreButtonTrailing
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
                    SettingsButtonRow(
                        icon = R.drawable.hard_drive_2_24,
                        text = "Восстановить выбранное",
                        value = if (isIdle) "Восстановить" else "Идет...",
                        onClick = onAskRestoreMode
                    )
                }
            }
        }
        SettingsDivider()
        BackupConsole(
            lines = backup.console,
            onClear = onClearConsole
        )
    }

    if (showCreatePasswordDialog) {
        BackupCreatePasswordDialog(
            onDismiss = onDismissCreatePasswordDialog,
            onConfirm = { password ->
                showCreatePasswordDialog = false
                backup.setCreatePassword(password)
                createBackupLauncher.launch(XlrBackupManager.defaultFileName())
            }
        )
    }

    if (showRestorePasswordDialog) {
        BackupRestorePasswordDialog(
            errorMessage = backup.restorePasswordError,
            onDismiss = onDismissRestorePasswordDialog,
            onConfirm = { password -> backup.decryptArchive(password) }
        )
    }

    if (showRestoreModeDialog) {
        BackupRestoreModeDialog(
            summary = restoreSummaryText,
            onDismiss = onDismissRestoreModeDialog,
            onSelected = onRestore
        )
    }
}

/** Движок для превью: диска нет, папок нет. */
private object PreviewBackupEngine : BackupEngine {
    override suspend fun currentItems(options: XlrBackupOptions): List<XlrBackupItem> = emptyList()

    override suspend fun create(
        uri: String,
        paths: Set<String>,
        options: XlrBackupOptions,
        password: CharArray,
    ): Result<XlrBackupReport> = Result.success(XlrBackupReport.EMPTY)

    override suspend fun detectType(uri: String): XlrBackupType = XlrBackupType.UNSUPPORTED

    override suspend fun inspect(uri: String, password: CharArray?): Result<List<XlrBackupItem>> =
        Result.success(emptyList())

    override suspend fun restore(
        uri: String,
        paths: Set<String>,
        password: CharArray?,
        mode: XlrRestoreMode,
    ): Result<XlrBackupReport> = Result.success(XlrBackupReport.EMPTY)

    override suspend fun afterRestore(paths: Set<String>, log: (String) -> Unit) = Unit
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun BackupSettingsSectionPreview() = SettingsPreview {
    BackupSettingsSection(
        controller = null,
        onDataChanged = {}
    )
}
