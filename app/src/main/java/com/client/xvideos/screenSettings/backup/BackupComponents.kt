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
import androidx.compose.ui.draw.rotate
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

@Composable
internal fun BackupModeSelector(
    selected: BackupFlowScreen,
    enabled: Boolean,
    onSelected: (BackupFlowScreen) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        BackupFlowScreen.entries.forEachIndexed { index, item ->
            SegmentedButton(
                enabled = enabled,
                selected = selected == item,
                onClick = { onSelected(item) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = BackupFlowScreen.entries.size,
                    baseShape = RoundedCornerShape(8.dp)
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = SettingsAccentColor,
                    activeContentColor = SettingsScreenBackground,
                    activeBorderColor = SettingsAccentColor,
                    inactiveContainerColor = SettingsCardColor,
                    inactiveContentColor = Theme.L.textColor,
                    inactiveBorderColor = SettingsDividerColor
                ),
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
    onValueChange: (XlrBackupContentMode) -> Unit
) {
    SettingsValueRow(
        icon = R.drawable.hard_drive_2_24,
        text = title,
        value = "${backupContentModeTitle(value)} • $description"
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        XlrBackupContentMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                enabled = enabled,
                selected = value == mode,
                onClick = { onValueChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = XlrBackupContentMode.entries.size
                ),
                label = { Text(backupContentModeTitle(mode)) }
            )
        }
    }
}

internal fun backupContentModeTitle(mode: XlrBackupContentMode): String {
    return when (mode) {
        XlrBackupContentMode.MINI -> "Мини"
        XlrBackupContentMode.FULL -> "Полный"
    }
}

@Composable
internal fun BackupConsole(
    lines: List<String>,
    onClear: () -> Unit
) {
    val visibleLines = remember(lines) {
        lines
            .ifEmpty { listOf("Пока пусто") }
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
        text = "Консоль backup",
        subtitle = if (lines.isEmpty()) "Здесь будет процесс восстановления файлов из сети" else "${visibleLines.size} строк",
        trailing = {
            TextButton(
                enabled = lines.isNotEmpty(),
                onClick = onClear
            ) {
                Text("Очистить", color = SettingsAccentColor)
            }
        }
    )
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(SettingsTopBarColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
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
internal fun BackupConsoleLine(line: String) {
    val lower = line.lowercase()
    val isSummary = line.startsWith("---------") || lower.contains("итог")
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
        modifier = Modifier.padding(vertical = if (isSummary) 2.dp else 1.dp)
    )
}

@Composable
internal fun BackupSelectionActions(
    enabled: Boolean,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            enabled = enabled,
            onClick = onSelectAll
        ) {
            Text("Все X/L/R")
        }
        Spacer(Modifier.width(8.dp))
        TextButton(
            enabled = enabled,
            onClick = onSelectNone
        ) {
            Text("Снять", color = SettingsAccentColor)
        }
    }
}

@Composable
internal fun BackupFolderList(
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit
) {
    if (items.isEmpty()) {
        SettingsValueRow(
            icon = R.drawable.hard_drive_2_24,
            text = "Папки",
            value = "Нет данных для backup"
        )
        return
    }

    var expandedSections by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val rootItems = remember(items) { items.filter { it.parentPath == null } }
    val childrenByParent = remember(items) {
        items.filter { it.parentPath != null }.groupBy { it.parentPath }
    }

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

@Composable
internal fun BackupSectionGroup(
    section: XlrBackupItem,
    children: List<XlrBackupItem>,
    items: List<XlrBackupItem>,
    selectedPaths: Set<String>,
    expanded: Boolean,
    enabled: Boolean,
    onToggleExpanded: () -> Unit,
    onToggle: (String) -> Unit
) {
    val state = backupSectionToggleState(items, selectedPaths, section)

    SettingsListItem(
        icon = backupItemIcon(section.section),
        text = section.title,
        subtitle = "${section.files} файлов • ${formatBytes(section.bytes)}",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        contentDescription = if (expanded) "Свернуть" else "Развернуть",
                        tint = SettingsAccentColor,
                        modifier = Modifier.rotate(if (expanded) 90f else 0f)
                    )
                }
            }
        },
        onClick = { if (enabled) onToggleExpanded() }
    )

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top)
    ) {
        Column {
            if (children.isEmpty()) {
                SettingsValueRow(
                    icon = backupItemIcon(section.section),
                    text = section.title,
                    value = "Нет вложенных папок"
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

@Composable
internal fun BackupCreatePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: CharArray) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordConfirmVisible by remember { mutableStateOf(false) }

    val isLengthValid = password.length >= 4
    val isMatching = password == passwordConfirm
    val isValid = isLengthValid && isMatching

    LavenderDialog(
        title = "Шифрование бэкапа",
        onDismiss = {
            password = ""
            passwordConfirm = ""
            onDismiss()
        },
        confirmText = "Создать",
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
                text = "Задайте пароль для шифрования архива. Без этого пароля восстановить данные будет невозможно.",
                style = Theme.L.Type.dialogBody.copy(color = dialogTheme.bodyColor),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль архива") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it },
                label = { Text("Подтверждение пароля") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordConfirmVisible = !passwordConfirmVisible }) {
                        Icon(
                            imageVector = if (passwordConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordConfirmVisible) "Скрыть пароль" else "Показать пароль",
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
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Пароль должен быть не короче 4 символов",
                    style = Theme.L.Type.dialogBody.copy(color = dialogTheme.buttonBackgroundDestructive),
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (passwordConfirm.isNotEmpty() && !isMatching) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Пароли не совпадают",
                    style = Theme.L.Type.dialogBody.copy(color = dialogTheme.buttonBackgroundDestructive),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
internal fun BackupRestorePasswordDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (password: CharArray) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isValid = password.isNotEmpty()

    LavenderDialog(
        title = "Ввод пароля бэкапа",
        onDismiss = {
            password = ""
            onDismiss()
        },
        confirmText = "Открыть",
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
                text = "Архив зашифрован. Введите пароль для расшифровки и чтения содержимого.",
                style = Theme.L.Type.dialogBody.copy(color = dialogTheme.bodyColor),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль архива") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
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
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    style = Theme.L.Type.dialogBody.copy(color = dialogTheme.buttonBackgroundDestructive),
                    modifier = Modifier.fillMaxWidth()
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
