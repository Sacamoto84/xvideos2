package com.client.xvideos.screenSettings.backup

import com.client.xvideos.R
import com.client.xvideos.common.theme.Theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.applock.DisableAppLockAutofill
import com.client.xvideos.common.ui.IncognitoKeyboard
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.backup.XlrBackupContentMode
import com.client.xvideos.common.backup.XlrBackupItem
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsCardColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsDividerColor
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsScreenBackground
import com.client.xvideos.screenSettings.components.SettingsTopBarColor
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.common.util.formatBytes

private val BACKUP_COMPONENT_CORNER = 8.dp
private val BACKUP_COMPONENT_SHAPE = RoundedCornerShape(BACKUP_COMPONENT_CORNER)
private val BACKUP_SELECTOR_HEIGHT = 56.dp
private val BACKUP_SELECTOR_HORIZONTAL_PADDING = 16.dp
private val BACKUP_SELECTOR_VERTICAL_PADDING = 6.dp
private val BACKUP_CONSOLE_HEIGHT = 260.dp
private val BACKUP_CONSOLE_HORIZONTAL_PADDING = 16.dp
private val BACKUP_CONSOLE_VERTICAL_PADDING = 8.dp
private val BACKUP_CONSOLE_INNER_PADDING = 10.dp
private val BACKUP_CONSOLE_SUMMARY_PADDING = 2.dp
private val BACKUP_CONSOLE_REGULAR_PADDING = 1.dp
private val BACKUP_SELECTION_START_PADDING = 72.dp
private val BACKUP_SELECTION_END_PADDING = 16.dp
private val BACKUP_SELECTION_TOP_PADDING = 4.dp
private val BACKUP_SELECTION_BOTTOM_PADDING = 8.dp
private val BACKUP_SELECTION_SPACER_WIDTH = 8.dp
private val BACKUP_DIALOG_SPACER_LARGE = 12.dp
private val BACKUP_DIALOG_SPACER_MEDIUM = 8.dp
private val BACKUP_DIALOG_SPACER_SMALL = 6.dp

private val BACKUP_SELECTOR_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .height(BACKUP_SELECTOR_HEIGHT)
    .padding(horizontal = BACKUP_SELECTOR_HORIZONTAL_PADDING, vertical = BACKUP_SELECTOR_VERTICAL_PADDING)
private val BACKUP_CONSOLE_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .height(BACKUP_CONSOLE_HEIGHT)
    .padding(horizontal = BACKUP_CONSOLE_HORIZONTAL_PADDING, vertical = BACKUP_CONSOLE_VERTICAL_PADDING)
    .background(SettingsTopBarColor, BACKUP_COMPONENT_SHAPE)
    .padding(BACKUP_CONSOLE_INNER_PADDING)
private val BACKUP_SELECTION_ACTIONS_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(
        start = BACKUP_SELECTION_START_PADDING,
        end = BACKUP_SELECTION_END_PADDING,
        top = BACKUP_SELECTION_TOP_PADDING,
        bottom = BACKUP_SELECTION_BOTTOM_PADDING
    )
private val BACKUP_SELECTION_SPACER_MODIFIER = Modifier.width(BACKUP_SELECTION_SPACER_WIDTH)
private val BACKUP_DIALOG_SPACER_LARGE_MODIFIER = Modifier.height(BACKUP_DIALOG_SPACER_LARGE)
private val BACKUP_DIALOG_SPACER_MEDIUM_MODIFIER = Modifier.height(BACKUP_DIALOG_SPACER_MEDIUM)
private val BACKUP_DIALOG_SPACER_SMALL_MODIFIER = Modifier.height(BACKUP_DIALOG_SPACER_SMALL)
private val DIALOG_FIELD_MODIFIER = Modifier.fillMaxWidth()

private const val TEXT_BACKUP_CONSOLE = "Консоль backup"
private const val TEXT_CONSOLE_EMPTY = "Пока пусто"
private const val TEXT_CONSOLE_DEFAULT_SUBTITLE = "Здесь будет процесс восстановления файлов из сети"
private const val TEXT_CONSOLE_CLEAR = "Очистить"
private const val MODE_TITLE_MINI = "Мини"
private const val MODE_TITLE_FULL = "Полный"
private const val TEXT_SELECT_ALL = "Все X/L/R"
private const val TEXT_DESELECT_ALL = "Снять"
private const val TEXT_FOLDERS = "Папки"
private const val TEXT_NO_DATA_FOR_BACKUP = "Нет данных для backup"
private const val CONSOLE_SUMMARY_PREFIX = "---------"
private const val CONSOLE_KEYWORD_TOTAL = "итог"

private const val CHEVRON_ROTATION_EXPANDED = 90f
private const val CHEVRON_ROTATION_COLLAPSED = 0f
private const val TEXT_COLLAPSE = "Свернуть"
private const val TEXT_EXPAND = "Развернуть"
private const val TEXT_NO_CHILD_FOLDERS = "Нет вложенных папок"

private const val MIN_PASSWORD_LENGTH = 4
private const val TEXT_CREATE_PASSWORD_TITLE = "Шифрование бэкапа"
private const val TEXT_CREATE_PASSWORD_CONFIRM = "Создать"
private const val TEXT_CREATE_PASSWORD_DESCRIPTION = "Задайте пароль для шифрования архива. Без этого пароля восстановить данные будет невозможно."
private const val TEXT_PASSWORD_LABEL = "Пароль архива"
private const val TEXT_PASSWORD_CONFIRM_LABEL = "Подтверждение пароля"
private const val TEXT_PASSWORD_TOO_SHORT = "Пароль должен быть не короче 4 символов"
private const val TEXT_PASSWORDS_DO_NOT_MATCH = "Пароли не совпадают"
private const val TEXT_PASSWORD_HIDE = "Скрыть пароль"
private const val TEXT_PASSWORD_SHOW = "Показать пароль"
private const val TEXT_RESTORE_PASSWORD_TITLE = "Ввод пароля бэкапа"
private const val TEXT_RESTORE_PASSWORD_CONFIRM = "Открыть"
private const val TEXT_RESTORE_PASSWORD_DESCRIPTION = "Архив зашифрован. Введите пароль для расшифровки и чтения содержимого."

private val BACKUP_FLOW_SCREEN_COUNT = BackupFlowScreen.entries.size
private val XLR_BACKUP_CONTENT_MODE_COUNT = XlrBackupContentMode.entries.size
private val BACKUP_SECTION_ENTER = expandVertically(expandFrom = Alignment.Top)
private val BACKUP_SECTION_EXIT = shrinkVertically(shrinkTowards = Alignment.Top)
private val ROW_VERTICAL_ALIGNMENT_CENTER = Alignment.CenterVertically

@Composable
internal fun BackupModeSelector(
    selected: BackupFlowScreen,
    enabled: Boolean,
    onSelected: (BackupFlowScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segmentedColors = SegmentedButtonDefaults.colors(
        activeContainerColor = SettingsAccentColor,
        activeContentColor = SettingsScreenBackground,
        activeBorderColor = SettingsAccentColor,
        inactiveContainerColor = SettingsCardColor,
        inactiveContentColor = Theme.L.textColor,
        inactiveBorderColor = SettingsDividerColor
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.then(BACKUP_SELECTOR_BASE_MODIFIER)
    ) {
        BackupFlowScreen.entries.forEachIndexed { index, item ->
            SegmentedButton(
                enabled = enabled,
                selected = selected == item,
                onClick = { onSelected(item) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = BACKUP_FLOW_SCREEN_COUNT,
                    baseShape = BACKUP_COMPONENT_SHAPE
                ),
                colors = segmentedColors,
                label = {
                    Text(
                        text = item.title,
                        color = if (selected == item) SettingsScreenBackground else SettingsRowTextPrimary,
                        style = Theme.L.Type.button
                    )
                }
            )
        }
    }
}

@Composable
internal fun BackupContentModeSelector(
    title: String,
    value: XlrBackupContentMode,
    enabled: Boolean,
    description: String,
    onValueChange: (XlrBackupContentMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsValueRow(
        icon = R.drawable.hard_drive_2_24,
        text = title,
        value = "${backupContentModeTitle(value)} • $description"
    )
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.then(BACKUP_SELECTOR_BASE_MODIFIER)
    ) {
        XlrBackupContentMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                enabled = enabled,
                selected = value == mode,
                onClick = { onValueChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = XLR_BACKUP_CONTENT_MODE_COUNT
                ),
                label = { Text(backupContentModeTitle(mode)) }
            )
        }
    }
}

internal fun backupContentModeTitle(mode: XlrBackupContentMode): String {
    return when (mode) {
        XlrBackupContentMode.MINI -> MODE_TITLE_MINI
        XlrBackupContentMode.FULL -> MODE_TITLE_FULL
    }
}

@Composable
internal fun BackupConsole(
    lines: List<String>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleLines = remember(lines) {
        lines
            .ifEmpty { listOf(TEXT_CONSOLE_EMPTY) }
            .flatMap { entry -> entry.lineSequence().toList() }
    }

    // LazyColumn, а не Column в verticalScroll: буфер поднят до
    // BACKUP_CONSOLE_MAX_LINES, и рисовать столько строк разом незачем —
    // компонуются только видимые.
    val listState = rememberLazyListState()

    // Прокрутка к последней строке по мере поступления. Без этого разросшийся
    // буфер бесполезен: смотришь на начало, а работа идёт в конце.
    LaunchedEffect(visibleLines.size) {
        if (visibleLines.isNotEmpty()) {
            listState.scrollToItem(visibleLines.lastIndex)
        }
    }

    SettingsListItem(
        icon = R.drawable.hard_drive_2_24,
        text = TEXT_BACKUP_CONSOLE,
        subtitle = if (lines.isEmpty()) TEXT_CONSOLE_DEFAULT_SUBTITLE else "${visibleLines.size} строк",
        trailing = {
            TextButton(
                enabled = lines.isNotEmpty(),
                onClick = onClear
            ) {
                Text(TEXT_CONSOLE_CLEAR, color = SettingsAccentColor)
            }
        }
    )
    LazyColumn(
        state = listState,
        modifier = modifier.then(BACKUP_CONSOLE_BASE_MODIFIER)
    ) {
        items(
            count = visibleLines.size,
            key = { index -> index }
        ) { index ->
            BackupConsoleLine(visibleLines[index])
        }
    }
}

@Composable
internal fun BackupConsoleLine(
    line: String,
    modifier: Modifier = Modifier,
) {
    val lower = line.lowercase()
    val isSummary = line.startsWith(CONSOLE_SUMMARY_PREFIX) || lower.contains(CONSOLE_KEYWORD_TOTAL)
    val isError = lower.contains("ошиб") ||
            lower.contains("бит") ||
            lower.contains("не скачан") ||
            lower.contains("failed")
    val isSuccess = lower.contains("скачано") ||
            lower.contains("очередь") ||
            lower.contains("готов") ||
            lower.contains("создан") ||
            lower.contains("восстановлен")
    val isWarning = lower.contains("пропущено") ||
            lower.contains("нет ")
    val color = when {
        isSummary -> SettingsAccentColor
        isError -> Theme.L.r0
        isSuccess -> Theme.L.g0
        isWarning -> Theme.L.lavender
        else -> SettingsRowTextPrimary
    }
    val style = if (isSummary) {
        Theme.L.Type.rowTitle.copy(color = color)
    } else {
        Theme.L.Type.rowSubtitle.copy(color = color)
    }

    Text(
        text = line,
        color = color,
        style = style,
        modifier = modifier.padding(vertical = if (isSummary) BACKUP_CONSOLE_SUMMARY_PADDING else BACKUP_CONSOLE_REGULAR_PADDING)
    )
}

@Composable
internal fun BackupSelectionActions(
    enabled: Boolean,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.then(BACKUP_SELECTION_ACTIONS_BASE_MODIFIER),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            enabled = enabled,
            onClick = onSelectAll
        ) {
            Text(TEXT_SELECT_ALL)
        }
        Spacer(BACKUP_SELECTION_SPACER_MODIFIER)
        TextButton(
            enabled = enabled,
            onClick = onSelectNone
        ) {
            Text(TEXT_DESELECT_ALL, color = SettingsAccentColor)
        }
    }
}

@Composable
internal fun BackupFolderList(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Column(modifier = modifier) {
            SettingsValueRow(
                icon = R.drawable.hard_drive_2_24,
                text = TEXT_FOLDERS,
                value = TEXT_NO_DATA_FOR_BACKUP
            )
        }
        return
    }

    var expandedSections by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val rootItems = remember(items) { items.filter { it.parentPath == null } }
    val childrenByParent = remember(items) {
        items.filter { it.parentPath != null }.groupBy { it.parentPath }
    }

    Column(modifier = modifier) {
        rootItems.forEachIndexed { index, section ->
            val children = childrenByParent[section.path].orEmpty()
            if (index > 0) SettingsDivider()
            BackupSectionGroup(
                section = section,
                children = children,
                items = items,
                selectedPaths = selectedPaths,
                expanded = section.path in expandedSections,
                enabled = enabled,
                onToggleExpanded = {
                    expandedSections = if (section.path in expandedSections) {
                        expandedSections - section.path
                    } else {
                        expandedSections + section.path
                    }
                },
                onToggle = onToggle
            )
        }
    }
}

@Composable
internal fun BackupSectionGroup(
    section: XlrBackupItem,
    children: List<XlrBackupItem>,
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    expanded: Boolean,
    enabled: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = backupSectionToggleState(items, selectedPaths, section)

    Column(modifier = modifier) {
        SettingsListItem(
            icon = backupItemIcon(section.section),
            text = section.title,
            subtitle = "${section.files} файлов • ${formatBytes(section.bytes)}",
            trailing = {
                Row(verticalAlignment = ROW_VERTICAL_ALIGNMENT_CENTER) {
                    TriStateCheckbox(
                        state = state,
                        enabled = enabled,
                        onClick = { onToggle(section.path) }
                    )
                    TextButton(
                        enabled = enabled,
                        onClick = onToggleExpanded
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.exo_ic_chevron_right),
                            contentDescription = if (expanded) TEXT_COLLAPSE else TEXT_EXPAND,
                            tint = SettingsAccentColor,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (expanded) CHEVRON_ROTATION_EXPANDED else CHEVRON_ROTATION_COLLAPSED
                            }
                        )
                    }
                }
            },
            onClick = { if (enabled) onToggleExpanded() }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = BACKUP_SECTION_ENTER,
            exit = BACKUP_SECTION_EXIT
        ) {
            Column {
                if (children.isEmpty()) {
                    SettingsValueRow(
                        icon = backupItemIcon(section.section),
                        text = section.title,
                        value = TEXT_NO_CHILD_FOLDERS
                    )
                } else {
                    children.forEach { child ->
                        SettingsDivider()
                        SettingsListItem(
                            icon = backupItemIcon(child.section),
                            text = backupItemTitle(child),
                            subtitle = "${child.path} • ${child.files} файлов • ${formatBytes(child.bytes)}",
                            trailing = {
                                Checkbox(
                                    checked = isBackupPathChecked(items, selectedPaths, child),
                                    enabled = enabled,
                                    onCheckedChange = { onToggle(child.path) }
                                )
                            },
                            onClick = { if (enabled) onToggle(child.path) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BackupCreatePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: CharArray) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordConfirmVisible by remember { mutableStateOf(false) }

    val isLengthValid = password.length >= MIN_PASSWORD_LENGTH
    val isMatching = password == passwordConfirm
    val isValid = isLengthValid && isMatching

    LavenderDialog(
        title = TEXT_CREATE_PASSWORD_TITLE,
        onDismiss = {
            password = ""
            passwordConfirm = ""
            onDismiss()
        },
        confirmText = TEXT_CREATE_PASSWORD_CONFIRM,
        confirmEnabled = isValid,
        onConfirm = {
            if (isValid) {
                val chars = password.toCharArray()
                password = ""
                passwordConfirm = ""
                onConfirm(chars)
            }
        },
        content = {
            DisableAppLockAutofill()
            val dialogTheme = Theme.DialogLavande
            Text(
                text = TEXT_CREATE_PASSWORD_DESCRIPTION,
                style = Theme.L.Type.dialogBody.copy(color = dialogTheme.bodyColor),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = BACKUP_DIALOG_SPACER_LARGE_MODIFIER)

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(TEXT_PASSWORD_LABEL) },
                singleLine = true,
                modifier = DIALOG_FIELD_MODIFIER,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) TEXT_PASSWORD_HIDE else TEXT_PASSWORD_SHOW,
                            tint = dialogTheme.bodyColor
                        )
                    }
                },
                keyboardOptions = IncognitoKeyboard.options(
                    forceIncognito = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = BACKUP_DIALOG_SPACER_MEDIUM_MODIFIER)

            OutlinedTextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it },
                label = { Text(TEXT_PASSWORD_CONFIRM_LABEL) },
                singleLine = true,
                modifier = DIALOG_FIELD_MODIFIER,
                visualTransformation = if (passwordConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordConfirmVisible = !passwordConfirmVisible }) {
                        Icon(
                            imageVector = if (passwordConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordConfirmVisible) TEXT_PASSWORD_HIDE else TEXT_PASSWORD_SHOW,
                            tint = dialogTheme.bodyColor
                        )
                    }
                },
                keyboardOptions = IncognitoKeyboard.options(
                    forceIncognito = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                )
            )

            if (password.isNotEmpty() && !isLengthValid) {
                Spacer(modifier = BACKUP_DIALOG_SPACER_SMALL_MODIFIER)
                Text(
                    text = TEXT_PASSWORD_TOO_SHORT,
                    style = Theme.L.Type.dialogBody.copy(color = dialogTheme.buttonBackgroundDestructive),
                    modifier = DIALOG_FIELD_MODIFIER
                )
            } else if (passwordConfirm.isNotEmpty() && !isMatching) {
                Spacer(modifier = BACKUP_DIALOG_SPACER_SMALL_MODIFIER)
                Text(
                    text = TEXT_PASSWORDS_DO_NOT_MATCH,
                    style = Theme.L.Type.dialogBody.copy(color = dialogTheme.buttonBackgroundDestructive),
                    modifier = DIALOG_FIELD_MODIFIER
                )
            }
        }
    )
}

@Composable
internal fun BackupRestorePasswordDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (password: CharArray) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isValid = password.isNotEmpty()

    LavenderDialog(
        title = TEXT_RESTORE_PASSWORD_TITLE,
        onDismiss = {
            password = ""
            onDismiss()
        },
        confirmText = TEXT_RESTORE_PASSWORD_CONFIRM,
        confirmEnabled = isValid,
        onConfirm = {
            if (isValid) {
                val chars = password.toCharArray()
                password = ""
                onConfirm(chars)
            }
        },
        content = {
            DisableAppLockAutofill()
            val dialogTheme = Theme.DialogLavande
            Text(
                text = TEXT_RESTORE_PASSWORD_DESCRIPTION,
                style = Theme.L.Type.dialogBody.copy(color = dialogTheme.bodyColor),
                modifier = DIALOG_FIELD_MODIFIER
            )
            Spacer(modifier = BACKUP_DIALOG_SPACER_LARGE_MODIFIER)

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(TEXT_PASSWORD_LABEL) },
                singleLine = true,
                modifier = DIALOG_FIELD_MODIFIER,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) TEXT_PASSWORD_HIDE else TEXT_PASSWORD_SHOW,
                            tint = dialogTheme.bodyColor
                        )
                    }
                },
                keyboardOptions = IncognitoKeyboard.options(
                    forceIncognito = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (isValid) {
                        val chars = password.toCharArray()
                        password = ""
                        onConfirm(chars)
                    }
                })
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = BACKUP_DIALOG_SPACER_SMALL_MODIFIER)
                Text(
                    text = errorMessage,
                    style = Theme.L.Type.dialogBody.copy(color = dialogTheme.buttonBackgroundDestructive),
                    modifier = DIALOG_FIELD_MODIFIER
                )
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun BackupModeSelectorPreview() = SettingsPreview {
    var mode by remember { mutableStateOf(BackupFlowScreen.CREATE) }
    BackupModeSelector(selected = mode, enabled = true, onSelected = { mode = it })
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun BackupContentModeSelectorPreview() = SettingsPreview {
    var mode by remember { mutableStateOf(XlrBackupContentMode.MINI) }
    BackupContentModeSelector(
        title = "Режим L",
        value = mode,
        enabled = true,
        description = "Только метаданные",
        onValueChange = { mode = it }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun BackupConsolePreview() = SettingsPreview {
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

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun BackupSelectionActionsPreview() = SettingsPreview {
    BackupSelectionActions(enabled = true, onSelectAll = {}, onSelectNone = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun BackupFolderListPreview() = SettingsPreview {
    val items = listOf(
        XlrBackupItem(section = "X", path = "X", title = "XVideos", parentPath = null, files = 120, bytes = 300_000_000L),
        XlrBackupItem(section = "X", path = "X/Favorites", title = "Избранное", parentPath = "X", files = 100, bytes = 250_000_000L),
        XlrBackupItem(section = "L", path = "L", title = "Luscious", parentPath = null, files = 45, bytes = 80_000_000L)
    )
    var selected by remember { mutableStateOf(setOf("X", "X/Favorites", "L")) }
    BackupFolderList(
        items = items,
        selectedPaths = selected,
        enabled = true,
        onToggle = { path ->
            selected = if (path in selected) selected - path else selected + path
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun BackupCreatePasswordDialogPreview() = SettingsPreview {
    BackupCreatePasswordDialog(onDismiss = {}, onConfirm = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun BackupRestorePasswordDialogPreview() = SettingsPreview {
    BackupRestorePasswordDialog(errorMessage = "Неверный пароль архива", onDismiss = {}, onConfirm = {})
}
