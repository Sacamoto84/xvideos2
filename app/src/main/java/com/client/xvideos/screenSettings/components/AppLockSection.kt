package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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

@Composable
fun AppLockSettingsSection() {
    val context = LocalContext.current.applicationContext
    val appLockEnabled = Settings.app_lock_enabled.field.collectAsStateWithLifecycle().value
    var passwordSet by remember { mutableStateOf(AppLockRepository.isPasswordSet(context)) }
    var dialogMode by remember { mutableStateOf<AppLockDialogMode?>(null) }
    val enabled = appLockEnabled && passwordSet
    val scope = rememberCoroutineScope()
    var showCamouflageVerificationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(appLockEnabled, passwordSet) {
        if (appLockEnabled && !passwordSet) {
            Settings.app_lock_enabled.setValue(false)
        }
        if (!passwordSet && Settings.camouflage_calculator_enabled.field.value) {
            Settings.camouflage_calculator_enabled.setValue(false)
            LauncherAliasManager.setCalculatorAliasEnabled(context, false)
        }
    }

    dialogMode?.let { mode ->
        AppLockPasswordDialog(
            mode = mode,
            onDismiss = { dialogMode = null },
            onComplete = {
                passwordSet = AppLockRepository.isPasswordSet(context)
                dialogMode = null
            }
        )
    }

    if (showCamouflageVerificationDialog) {
        CamouflageVerificationDialog(
            onDismiss = { showCamouflageVerificationDialog = false },
            onSuccess = { showCamouflageVerificationDialog = false }
        )
    }

    SettingsGroup {
        SettingsListItem(
            icon = R.drawable.key_24,
            text = "Блокировка при запуске",
            subtitle = if (enabled) "Включена" else "Выключена",
            trailing = {
                Button(
                    onClick = { dialogMode = if (enabled) AppLockDialogMode.CHANGE else AppLockDialogMode.SET }
                ) {
                    Text(if (enabled) "Изменить" else "Задать")
                }
            }
        )

        if (enabled) {
            SettingsDivider2()
            SettingsListItem(
                icon = R.drawable.icon_red,
                text = "Код доступа",
                subtitle = "Отключить блокировку приложения",
                trailing = {
                    TextButton(onClick = { dialogMode = AppLockDialogMode.DISABLE }) {
                        Text("Отключить", color = Color(0xFFFF7A7A))
                    }
                }
            )
        }

        SettingsDivider2()

        CamouflageGroup(
            passwordSet = passwordSet,
            onEnableRequested = { showCamouflageVerificationDialog = true }
        )

        SettingsDivider2()

        val keyboardIncognito = Settings.keyboard_incognito_enabled.field.collectAsStateWithLifecycle().value

        SettingsSwitchRow(
            icon = R.drawable.memory_24,
            text = "Инкогнито-клавиатура",
            subtitle = if (keyboardIncognito) "Клавиатура не сохраняет поисковые запросы" else "Стандартный режим ввода",
            value = keyboardIncognito,
            onValueChange = { Settings.keyboard_incognito_enabled.setValue(it) }
        )

        SettingsDivider2()

        val blurRecentTasks = Settings.blur_recent_tasks.field.collectAsStateWithLifecycle().value

        SettingsSwitchRow(
            icon = R.drawable.ic_blur_24,
            text = "Защита в диспетчере задач",
            subtitle = if (blurRecentTasks) {
                "Превью скрыто/размыто в карусели недавних задач"
            } else {
                "Отображается обычный снимок экрана"
            },
            value = blurRecentTasks,
            onValueChange = { Settings.blur_recent_tasks.setValue(it) }
        )
    }
}

@Composable
private fun CamouflageGroup(
    passwordSet: Boolean,
    onEnableRequested: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val isCamouflage = Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle().value
    val camouflageSubtitle = when {
        !passwordSet -> "Сначала задайте код доступа"
        isCamouflage -> "Иконка «Калькулятор», секретный вход по PIN + «=»"
        else -> "Выключена (стандартная иконка приложения)"
    }

    SettingsSwitchRow(
            icon = R.drawable.ic_launcher_calculator,
            text = "Маскировка под калькулятор",
            subtitle = camouflageSubtitle,
            value = isCamouflage && passwordSet,
            enabled = passwordSet,
            onValueChange = { enable ->
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
        )

}

@Composable
private fun CamouflageVerificationDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var pinInput by rememberSaveable { mutableStateOf("") }
    var pinError by rememberSaveable { mutableStateOf<String?>(null) }
    var isVerifyingPin by remember { mutableStateOf(false) }

    LavenderDialog(
        title = "Включение маскировки",
        onDismiss = onDismiss,
        content = {
            DisableAppLockAutofill()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Для работы маскировки под калькулятор код доступа должен состоять только из цифр. Введите ваш текущий PIN-код для подтверждения:",
                    style = Theme.L.Type.dialogBody.copy(color = Theme.DialogLavande.bodyColor)
                )
                PasswordSettingField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        pinError = null
                    },
                    label = "Числовой PIN-код",
                    keyboardType = KeyboardType.NumberPassword,
                    onDone = {}
                )
                pinError?.let {
                    val errorColor = Color(0xFFB3261E)
                    Text(it, color = errorColor, style = Theme.L.Type.dialogBody.copy(color = errorColor))
                }
            }
        },
        confirmText = "Включить",
        confirmEnabled = pinInput.length >= 4 && !isVerifyingPin,
        onConfirm = {
            if (isVerifyingPin) return@LavenderDialog
            if (!pinInput.all { it.isDigit() }) {
                pinError = "Код доступа для калькулятора должен состоять только из цифр"
                return@LavenderDialog
            }
            scope.launch {
                isVerifyingPin = true
                try {
                    val ok = AppLockRepository.verifyPassword(context, pinInput)
                    if (ok) {
                        Settings.camouflage_calculator_enabled.setValue(true)
                        LauncherAliasManager.setCalculatorAliasEnabled(context, true)
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
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun AppLockSettingsSectionPreview() = SettingsPreview {
    AppLockSettingsSection()
}

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

    val isCamouflage = Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle().value
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

                if (isCamouflage && needsNewPassword && !newPassword.all { it.isDigit() }) {
                    errorText = "При включённой маскировке код доступа должен состоять только из цифр"
                    return@launch
                }

                when (mode) {
                    AppLockDialogMode.SET -> {
                        AppLockRepository.setPassword(context, newPassword).onSuccess {
                            SnackBar.success("Код доступа включён")
                            onComplete()
                        }.onFailure {
                            errorText = it.message ?: "Не удалось сохранить код доступа"
                        }
                    }
                    AppLockDialogMode.CHANGE -> {
                        AppLockRepository.setPassword(context, newPassword).onSuccess {
                            SnackBar.success("Код доступа изменён")
                            onComplete()
                        }.onFailure {
                            errorText = it.message ?: "Не удалось изменить код доступа"
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

    LavenderDialog(
        title = when (mode) {
            AppLockDialogMode.SET -> "Задать код доступа"
            AppLockDialogMode.CHANGE -> "Изменить код доступа"
            AppLockDialogMode.DISABLE -> "Отключить код доступа"
        },
        onDismiss = onDismiss,
        content = {
            DisableAppLockAutofill()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (needsCurrentPassword) {
                    PasswordSettingField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            errorText = null
                        },
                        label = "Текущий код доступа",
                        keyboardType = passwordKeyboardType,
                        onDone = { if (canSubmit) submit() }
                    )
                }

                if (needsNewPassword) {
                    PasswordSettingField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorText = null
                        },
                        label = "Новый код доступа",
                        keyboardType = passwordKeyboardType,
                        onDone = { if (canSubmit) submit() }
                    )
                    PasswordSettingField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorText = null
                        },
                        label = "Повтор кода доступа",
                        keyboardType = passwordKeyboardType,
                        onDone = { if (canSubmit) submit() }
                    )
                }

                errorText?.let {
                    // Тёмно-красный, а не светлый: диалог стоит на светлом фоне,
                    // прежний #FF7A7A на нём почти не читался.
                    val errorColor = Color(0xFFB3261E)
                    Text(it, color = errorColor, style = Theme.L.Type.dialogBody.copy(color = errorColor))
                }
            }
        },
        confirmText = when (mode) {
            AppLockDialogMode.DISABLE -> "Отключить"
            else -> "Сохранить"
        },
        onConfirm = { submit() },
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    DisableAppLockAutofill()

    var showPassword by rememberSaveable { mutableStateOf(false) }
    val d = Theme.DialogLavande

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation =
            if (showPassword) VisualTransformation.None else AccessCodeVisualTransformation,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
        textStyle = Theme.L.Type.body.copy(color = d.bodyColor),
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    imageVector =
                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription =
                        if (showPassword) "Скрыть код доступа" else "Показать код доступа",
                    tint = d.dismissTextColor
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = d.bodyColor,
            unfocusedTextColor = d.bodyColor,
            cursorColor = d.dismissTextColor,
            focusedBorderColor = d.dismissTextColor,
            unfocusedBorderColor = Color(0xFF9A9A9A),
            focusedLabelColor = d.dismissTextColor,
            unfocusedLabelColor = Color(0xFF6E6E6E)
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
