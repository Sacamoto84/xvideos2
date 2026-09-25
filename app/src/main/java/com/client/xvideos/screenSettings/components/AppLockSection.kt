package com.client.xvideos.screenSettings.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.client.xvideos.common.applock.AppLockTimeout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.R
import com.client.xvideos.calculator.LauncherAliasManager
import com.client.xvideos.common.applock.AccessCodeVisualTransformation
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.DisableAppLockAutofill
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import kotlinx.coroutines.launch

internal enum class AppLockDialogMode { SET, CHANGE, DISABLE }

private val appLockErrorColor = Color(0xFFB3261E)
private val appLockDisableColor = Color(0xFFFF7A7A)
private val TIMEOUT_ITEM_SHAPE = RoundedCornerShape(12.dp)
private val UNFOCUSED_BORDER_COLOR = Color(0xFF9A9A9A)
private val UNFOCUSED_LABEL_COLOR = Color(0xFF6E6E6E)
private val RADIO_UNSELECTED_COLOR = Color(0xFF938F99)

private val RADIO_SPACER_WIDTH = 12.dp
private val DIALOG_ITEM_SPACING = 10.dp
private val DIALOG_COLUMN_VERTICAL_ARRANGEMENT = Arrangement.spacedBy(DIALOG_ITEM_SPACING)
private val TIMEOUT_ITEM_SPACING = 4.dp
private val TIMEOUT_ITEM_HORIZONTAL_PADDING = 8.dp
private val TIMEOUT_ITEM_VERTICAL_PADDING = 10.dp

private val TIMEOUT_COLUMN_BASE_MODIFIER = Modifier.fillMaxWidth()
private val TIMEOUT_COLUMN_VERTICAL_ARRANGEMENT = Arrangement.spacedBy(TIMEOUT_ITEM_SPACING)
private val TIMEOUT_ITEM_FULL_MODIFIER = Modifier
    .fillMaxWidth()
    .clip(TIMEOUT_ITEM_SHAPE)
    .padding(horizontal = TIMEOUT_ITEM_HORIZONTAL_PADDING, vertical = TIMEOUT_ITEM_VERTICAL_PADDING)
private val RADIO_SPACER_MODIFIER = Modifier.width(RADIO_SPACER_WIDTH)
private val PASSWORD_FIELD_BASE_MODIFIER = Modifier.fillMaxWidth()
private val ICON_VISIBILITY = Icons.Filled.Visibility
private val ICON_VISIBILITY_OFF = Icons.Filled.VisibilityOff
private val ROW_CENTER_VERTICAL = Alignment.CenterVertically

private const val CD_HIDE_CODE = "Скрыть код доступа"
private const val CD_SHOW_CODE = "Показать код доступа"
private const val TEXT_APP_LOCK_TITLE = "Блокировка при запуске"
private const val TEXT_AUTO_LOCK = "Автоблокировка"
private const val TEXT_ACCESS_CODE = "Код доступа"
private const val TEXT_DISABLE_APP_LOCK = "Отключить блокировку приложения"
private const val TEXT_INCOGNITO_KEYBOARD = "Инкогнито-клавиатура"
private const val TEXT_BLUR_RECENT = "Защита в диспетчере задач"
private const val TEXT_CAMOUFLAGE = "Маскировка под калькулятор"
private const val BUTTON_CHANGE = "Изменить"
private const val BUTTON_SET = "Задать"
private const val BUTTON_DISABLE = "Отключить"
private const val SUBTITLE_ENABLED = "Включена"
private const val SUBTITLE_DISABLED = "Выключена"
private const val TIMEOUT_IMMEDIATELY = "Сразу при выходе"
private const val TIMEOUT_NEVER = "Выключена (только при перезапуске)"
private const val KEYBOARD_INCOGNITO_ACTIVE = "Клавиатура не сохраняет поисковые запросы"
private const val KEYBOARD_INCOGNITO_INACTIVE = "Стандартный режим ввода"
private const val BLUR_ACTIVE = "Превью скрыто/размыто в карусели недавних задач"
private const val BLUR_INACTIVE = "Отображается обычный снимок экрана"

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun AppLockSettingsSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val appLockEnabled by Settings.app_lock_enabled.field.collectAsStateWithLifecycle()
    val timeoutSeconds by Settings.app_lock_timeout_seconds.field.collectAsStateWithLifecycle()
    val keyboardIncognito by Settings.keyboard_incognito_enabled.field.collectAsStateWithLifecycle()
    val blurRecentTasks by Settings.blur_recent_tasks.field.collectAsStateWithLifecycle()
    var passwordSet by remember { mutableStateOf(AppLockRepository.isPasswordSet(context)) }
    var dialogMode by remember { mutableStateOf<AppLockDialogMode?>(null) }
    val enabled = appLockEnabled && passwordSet
    val scope = rememberCoroutineScope()
    var showCamouflageVerificationDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(appLockEnabled, passwordSet) {
        if (appLockEnabled && !passwordSet) {
            Settings.app_lock_enabled.setValue(false)
        }
        if (!passwordSet && Settings.camouflage_calculator_enabled.field.value) {
            Settings.camouflage_calculator_enabled.setValue(false)
            LauncherAliasManager.setCalculatorAliasEnabled(context, false)
        }
    }

    val onDismissDialogMode: () -> Unit = remember { { dialogMode = null } }
    val onDismissCamouflageVerification: () -> Unit = remember { { showCamouflageVerificationDialog = false } }
    val onDismissTimeoutDialog: () -> Unit = remember { { showTimeoutDialog = false } }
    val onSelectTimeout: (AppLockTimeout) -> Unit = remember {
        { timeout ->
            Settings.app_lock_timeout_seconds.setValue(timeout.seconds)
            showTimeoutDialog = false
        }
    }
    val onPasswordDialogComplete: () -> Unit = remember(context) {
        {
            passwordSet = AppLockRepository.isPasswordSet(context)
            dialogMode = null
        }
    }
    val onSetOrChangeLock: () -> Unit = remember(enabled) {
        { dialogMode = if (enabled) AppLockDialogMode.CHANGE else AppLockDialogMode.SET }
    }
    val onDisableLock: () -> Unit = remember { { dialogMode = AppLockDialogMode.DISABLE } }
    val onShowTimeoutClick: () -> Unit = remember { { showTimeoutDialog = true } }
    val onEnableCamouflageRequested: () -> Unit = remember { { showCamouflageVerificationDialog = true } }
    val onToggleIncognito: (Boolean) -> Unit = remember { { enabledValue -> Settings.keyboard_incognito_enabled.setValue(enabledValue) } }
    val onToggleBlurRecent: (Boolean) -> Unit = remember { { enabledValue -> Settings.blur_recent_tasks.setValue(enabledValue) } }

    val isAnyDialogOpen = dialogMode != null || showCamouflageVerificationDialog || showTimeoutDialog
    val onBackDismiss = remember {
        {
            dialogMode = null
            showCamouflageVerificationDialog = false
            showTimeoutDialog = false
        }
    }
    BackHandler(enabled = isAnyDialogOpen, onBack = onBackDismiss)

    dialogMode?.let { mode ->
        AppLockPasswordDialog(
            mode = mode,
            onDismiss = onDismissDialogMode,
            onComplete = onPasswordDialogComplete
        )
    }

    if (showCamouflageVerificationDialog) {
        CamouflageVerificationDialog(
            onDismiss = onDismissCamouflageVerification,
            onSuccess = onDismissCamouflageVerification
        )
    }

    val currentTimeout = remember(timeoutSeconds) { AppLockTimeout.fromSeconds(timeoutSeconds) }
    if (showTimeoutDialog) {
        AppLockTimeoutDialog(
            currentTimeout = currentTimeout,
            onDismiss = onDismissTimeoutDialog,
            onSelect = onSelectTimeout
        )
    }

    val setOrChangeTrailing: @Composable () -> Unit = remember(enabled, onSetOrChangeLock) {
        {
            Button(onClick = onSetOrChangeLock) {
                Text(if (enabled) BUTTON_CHANGE else BUTTON_SET)
            }
        }
    }

    val disableTrailing: @Composable () -> Unit = remember(onDisableLock) {
        {
            TextButton(onClick = onDisableLock) {
                Text(BUTTON_DISABLE, color = appLockDisableColor)
            }
        }
    }

    val lockSubtitle = remember(enabled) { if (enabled) SUBTITLE_ENABLED else SUBTITLE_DISABLED }
    val timeoutSubtitle = remember(currentTimeout) {
        when (currentTimeout) {
            AppLockTimeout.IMMEDIATELY -> TIMEOUT_IMMEDIATELY
            AppLockTimeout.NEVER -> TIMEOUT_NEVER
            else -> "Через ${currentTimeout.displayName.lowercase()} в фоне"
        }
    }
    val keyboardSubtitle = remember(keyboardIncognito) {
        if (keyboardIncognito) KEYBOARD_INCOGNITO_ACTIVE else KEYBOARD_INCOGNITO_INACTIVE
    }
    val blurSubtitle = remember(blurRecentTasks) {
        if (blurRecentTasks) {
            BLUR_ACTIVE
        } else {
            BLUR_INACTIVE
        }
    }

    SettingsGroup(modifier = modifier) {
        SettingsListItem(
            icon = R.drawable.key_24,
            text = TEXT_APP_LOCK_TITLE,
            subtitle = lockSubtitle,
            trailing = setOrChangeTrailing
        )

        if (enabled) {
            SettingsDivider2()
            SettingsListItem(
                icon = R.drawable.key_24,
                text = TEXT_AUTO_LOCK,
                subtitle = timeoutSubtitle,
                onClick = onShowTimeoutClick
            )

            SettingsDivider2()
            SettingsListItem(
                icon = R.drawable.key_24,
                text = TEXT_ACCESS_CODE,
                subtitle = TEXT_DISABLE_APP_LOCK,
                trailing = disableTrailing
            )
        }

        SettingsDivider2()

        CamouflageGroup(
            passwordSet = passwordSet,
            onEnableRequested = onEnableCamouflageRequested
        )

        SettingsDivider2()

        SettingsSwitchRow(
            icon = R.drawable.memory_24,
            text = TEXT_INCOGNITO_KEYBOARD,
            subtitle = keyboardSubtitle,
            value = keyboardIncognito,
            onValueChange = onToggleIncognito
        )

        SettingsDivider2()

        SettingsSwitchRow(
            icon = R.drawable.ic_blur_24,
            text = TEXT_BLUR_RECENT,
            subtitle = blurSubtitle,
            value = blurRecentTasks,
            onValueChange = onToggleBlurRecent
        )
    }
}

@Composable
private fun CamouflageGroup(
    passwordSet: Boolean,
    onEnableRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val isCamouflage by Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle()
    val camouflageSubtitle = remember(passwordSet, isCamouflage) {
        when {
            !passwordSet -> "Сначала задайте код доступа"
            isCamouflage -> "Иконка «Калькулятор», секретный вход по PIN + «=»"
            else -> "Выключена (стандартная иконка приложения)"
        }
    }
    val onToggleCamouflage: (Boolean) -> Unit = remember(passwordSet, onEnableRequested, context) {
        { enable ->
            if (passwordSet) {
                if (enable) {
                    onEnableRequested()
                } else {
                    Settings.camouflage_calculator_enabled.setValue(false)
                    LauncherAliasManager.setCalculatorAliasEnabled(context, false)
                    SnackBar.info("Маскировка под калькулятор отключена")
                }
            }
        }
    }

    SettingsSwitchRow(
        modifier = modifier,
        icon = R.drawable.ic_launcher_calculator,
        text = TEXT_CAMOUFLAGE,
        subtitle = camouflageSubtitle,
        value = isCamouflage && passwordSet,
        enabled = passwordSet,
        onValueChange = onToggleCamouflage
    )
}

@Composable
private fun CamouflageVerificationDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isVerifyingPin by remember { mutableStateOf(false) }

    val handleDismiss = remember(onDismiss) {
        {
            pinInput = ""
            pinError = null
            onDismiss()
        }
    }
    val onPinInputChange = remember {
        { value: String ->
            pinInput = value
            pinError = null
        }
    }
    val onDoneNoOp = remember { {} }
    val onConfirmVerification = remember(pinInput, isVerifyingPin, context, onSuccess) {
        {
            if (!isVerifyingPin) {
                if (!pinInput.all { ch -> ch.isDigit() }) {
                    pinError = "Код доступа для калькулятора должен состоять только из цифр"
                } else {
                    scope.launch {
                        isVerifyingPin = true
                        try {
                            val ok = AppLockRepository.verifyPassword(context, pinInput)
                            if (ok) {
                                Settings.camouflage_calculator_enabled.setValue(true)
                                LauncherAliasManager.setCalculatorAliasEnabled(context, true)
                                pinInput = ""
                                onSuccess()
                                SnackBar.success("Маскировка под калькулятор включена")
                            } else {
                                pinError = "Неверный код доступа. Если текущий пароль содержит буквы, сначала измените его на числовой PIN."
                            }
                        } finally {
                            isVerifyingPin = false
                        }
                    }
                }
            }
        }
    }

    LavenderDialog(
        title = "Включение маскировки",
        onDismiss = handleDismiss,
        content = {
            DisableAppLockAutofill()
            Column(verticalArrangement = Arrangement.spacedBy(DIALOG_ITEM_SPACING)) {
                Text(
                    "Для работы маскировки под калькулятор код доступа должен состоять только из цифр. Введите ваш текущий PIN-код для подтверждения:",
                    style = Theme.L.Type.dialogBody.copy(color = Theme.DialogLavande.bodyColor)
                )
                PasswordSettingField(
                    value = pinInput,
                    onValueChange = onPinInputChange,
                    label = "Числовой PIN-код",
                    keyboardType = KeyboardType.NumberPassword,
                    onDone = onDoneNoOp
                )
                pinError?.let { err ->
                    Text(err, color = appLockErrorColor, style = Theme.L.Type.dialogBody.copy(color = appLockErrorColor))
                }
            }
        },
        confirmText = "Включить",
        confirmEnabled = pinInput.length >= 4 && !isVerifyingPin,
        onConfirm = onConfirmVerification
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun CamouflageVerificationDialogPreview() = SettingsPreview {
    CamouflageVerificationDialog(
        onDismiss = {},
        onSuccess = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun AppLockSettingsSectionPreview() = SettingsPreview {
    AppLockSettingsSection()
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun AppLockPasswordDialog(
    mode: AppLockDialogMode,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    DisableAppLockAutofill()

    val context = LocalContext.current.applicationContext
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    // Пока считается хеш, повторное нажатие не должно запускать вторую проверку.
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isCamouflage by Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle()
    val passwordKeyboardType = if (isCamouflage) KeyboardType.NumberPassword else KeyboardType.Text

    val needsCurrentPassword = mode == AppLockDialogMode.CHANGE || mode == AppLockDialogMode.DISABLE
    val needsNewPassword = mode == AppLockDialogMode.SET || mode == AppLockDialogMode.CHANGE
    val canSubmit = when (mode) {
        AppLockDialogMode.SET -> newPassword.length >= 4 && confirmPassword.isNotBlank()
        AppLockDialogMode.CHANGE -> currentPassword.isNotBlank() && newPassword.length >= 4 && confirmPassword.isNotBlank()
        AppLockDialogMode.DISABLE -> currentPassword.isNotBlank()
    }

    // Проверка и установка кода — это 120 000 итераций PBKDF2 каждая. Раньше
    // они считались прямо в колбэке диалога, то есть на главном потоке.
    fun submit() {
        if (isSubmitting) return
        errorText = null

        scope.launch {
            isSubmitting = true
            try {
                if (needsCurrentPassword &&
                    !AppLockRepository.verifyPassword(context, currentPassword)
                ) {
                    errorText = "Текущий код доступа не подходит"
                    return@launch
                }

                if (needsNewPassword && newPassword != confirmPassword) {
                    errorText = "Коды доступа не совпадают"
                    return@launch
                }

                if (isCamouflage && needsNewPassword && !newPassword.all { ch -> ch.isDigit() }) {
                    errorText = "При включённой маскировке код доступа должен состоять только из цифр"
                    return@launch
                }

                when (mode) {
                    AppLockDialogMode.SET -> {
                        AppLockRepository.setPassword(context, newPassword).onSuccess {
                            SnackBar.success("Код доступа включён")
                            onComplete()
                        }.onFailure { error ->
                            errorText = error.message ?: "Не удалось сохранить код доступа"
                        }
                    }
                    AppLockDialogMode.CHANGE -> {
                        AppLockRepository.setPassword(context, newPassword).onSuccess {
                            SnackBar.success("Код доступа изменён")
                            onComplete()
                        }.onFailure { error ->
                            errorText = error.message ?: "Не удалось изменить код доступа"
                        }
                    }
                    AppLockDialogMode.DISABLE -> {
                        AppLockRepository.clearPassword(context)
                        SnackBar.success("Код доступа отключён")
                        onComplete()
                    }
                }
            } finally {
                isSubmitting = false
            }
        }
    }

    val onCurrentPasswordChange = remember {
        { value: String ->
            currentPassword = value
            errorText = null
        }
    }
    val onNewPasswordChange = remember {
        { value: String ->
            newPassword = value
            errorText = null
        }
    }
    val onConfirmPasswordChange = remember {
        { value: String ->
            confirmPassword = value
            errorText = null
        }
    }
    val onDoneSubmit = remember(canSubmit) {
        {
            if (canSubmit) submit()
        }
    }
    val onConfirmSubmit = remember { { submit() } }

    LavenderDialog(
        title = when (mode) {
            AppLockDialogMode.SET -> "Задать код доступа"
            AppLockDialogMode.CHANGE -> "Изменить код доступа"
            AppLockDialogMode.DISABLE -> "Отключить код доступа"
        },
        onDismiss = onDismiss,
        content = {
            DisableAppLockAutofill()
            Column(verticalArrangement = DIALOG_COLUMN_VERTICAL_ARRANGEMENT) {
                if (needsCurrentPassword) {
                    PasswordSettingField(
                        value = currentPassword,
                        onValueChange = onCurrentPasswordChange,
                        label = "Текущий код доступа",
                        keyboardType = passwordKeyboardType,
                        onDone = onDoneSubmit
                    )
                }

                if (needsNewPassword) {
                    PasswordSettingField(
                        value = newPassword,
                        onValueChange = onNewPasswordChange,
                        label = "Новый код доступа",
                        keyboardType = passwordKeyboardType,
                        onDone = onDoneSubmit
                    )
                    PasswordSettingField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = "Повтор кода доступа",
                        keyboardType = passwordKeyboardType,
                        onDone = onDoneSubmit
                    )
                }

                errorText?.let { err ->
                    Text(err, color = appLockErrorColor, style = Theme.L.Type.dialogBody.copy(color = appLockErrorColor))
                }
            }
        },
        confirmText = when (mode) {
            AppLockDialogMode.DISABLE -> "Отключить"
            else -> "Сохранить"
        },
        onConfirm = onConfirmSubmit,
        confirmEnabled = canSubmit,
        destructive = mode == AppLockDialogMode.DISABLE,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun AppLockPasswordDialogPreview() = SettingsPreview {
    AppLockPasswordDialog(
        mode = AppLockDialogMode.SET,
        onDismiss = {},
        onComplete = {}
    )
}

/**
 * Поле ввода кода доступа в диалогах настроек.
 *
 * Две правки после проверки на устройстве.
 *
 * Цвета. Текст был задан белым, а диалог [LavenderDialog] стоит на светлом фоне
 * — читать написанное было почти нечем. Теперь всё берётся из палитры самого
 * диалога: тёмный текст, сиреневый акцент на фокусе, серая рамка без него.
 *
 * Показ кода. Звёзды стояли всегда и переключателя не было, хотя на экране
 * замка он есть. Здесь кода не видит никто, кроме владельца телефона, а
 * вводить вслепую новый код и его повтор — лишний способ ошибиться.
 */
@Composable
fun PasswordSettingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onDone: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    DisableAppLockAutofill()

    var showPassword by remember { mutableStateOf(false) }
    val dialogTheme = Theme.DialogLavande
    val onToggleShowPassword = remember { { showPassword = !showPassword } }
    val keyboardOptions = remember(keyboardType) {
        androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        )
    }
    val keyboardActions = remember(onDone) {
        androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() })
    }

    val labelComposable: @Composable () -> Unit = remember(label) { { Text(label) } }
    val trailingIconComposable: @Composable () -> Unit = remember(showPassword, dialogTheme.dismissTextColor, onToggleShowPassword) {
        {
            IconButton(onClick = onToggleShowPassword) {
                Icon(
                    imageVector =
                        if (showPassword) ICON_VISIBILITY_OFF else ICON_VISIBILITY,
                    contentDescription =
                        if (showPassword) CD_HIDE_CODE else CD_SHOW_CODE,
                    tint = dialogTheme.dismissTextColor
                )
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.then(PASSWORD_FIELD_BASE_MODIFIER),
        label = labelComposable,
        singleLine = true,
        visualTransformation =
            if (showPassword) VisualTransformation.None else AccessCodeVisualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = Theme.L.Type.body.copy(color = dialogTheme.bodyColor),
        trailingIcon = trailingIconComposable,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = dialogTheme.bodyColor,
            unfocusedTextColor = dialogTheme.bodyColor,
            cursorColor = dialogTheme.dismissTextColor,
            focusedBorderColor = dialogTheme.dismissTextColor,
            unfocusedBorderColor = UNFOCUSED_BORDER_COLOR,
            focusedLabelColor = dialogTheme.dismissTextColor,
            unfocusedLabelColor = UNFOCUSED_LABEL_COLOR
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun PasswordSettingFieldPreview() = SettingsPreview {
    PasswordSettingField(
        value = "1234",
        onValueChange = {},
        label = "Код доступа",
        onDone = {}
    )
}

@Composable
internal fun AppLockTimeoutDialog(
    currentTimeout: AppLockTimeout,
    onDismiss: () -> Unit,
    onSelect: (AppLockTimeout) -> Unit
) {
    val radioColors = RadioButtonDefaults.colors(
        selectedColor = SettingsAccentColor,
        unselectedColor = RADIO_UNSELECTED_COLOR
    )

    LavenderDialog(
        title = "Автоблокировка",
        onDismiss = onDismiss,
        dismissText = "Отмена",
        content = {
            Column(
                modifier = TIMEOUT_COLUMN_BASE_MODIFIER,
                verticalArrangement = TIMEOUT_COLUMN_VERTICAL_ARRANGEMENT
            ) {
                AppLockTimeout.entries.forEach { timeout ->
                    key(timeout) {
                        AppLockTimeoutItem(
                            timeout = timeout,
                            isSelected = (timeout == currentTimeout),
                            radioColors = radioColors,
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AppLockTimeoutItem(
    timeout: AppLockTimeout,
    isSelected: Boolean,
    radioColors: androidx.compose.material3.RadioButtonColors,
    onSelect: (AppLockTimeout) -> Unit,
    modifier: Modifier = Modifier
) {
    val onClick = remember(timeout, onSelect) { { onSelect(timeout) } }
    val label = remember(timeout) {
        when (timeout) {
            AppLockTimeout.IMMEDIATELY -> "Сразу при выходе"
            AppLockTimeout.NEVER -> "Никогда (только при перезапуске)"
            else -> timeout.displayName
        }
    }

    Row(
        modifier = modifier
            .then(TIMEOUT_ITEM_FULL_MODIFIER)
            .clickable(onClick = onClick),
        verticalAlignment = ROW_CENTER_VERTICAL
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = radioColors
        )
        Spacer(RADIO_SPACER_MODIFIER)
        Text(
            text = label,
            style = Theme.L.Type.dialogBody.copy(
                color = if (isSelected) SettingsAccentColor else Theme.DialogLavande.bodyColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun AppLockTimeoutDialogPreview() = SettingsPreview {
    AppLockTimeoutDialog(
        currentTimeout = AppLockTimeout.MINUTES_1,
        onDismiss = {},
        onSelect = {}
    )
}

