package com.client.xvideos.screenSettings.components

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import com.client.xvideos.common.theme.LavenderDialog
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
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.applock.AccessCodeVisualTransformation
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.R
import com.client.xvideos.common.applock.DisableAppLockAutofill
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar

internal enum class AppLockDialogMode { SET, CHANGE, DISABLE }

@Composable
fun AppLockSettingsSection() {
    val context = LocalContext.current.applicationContext
    val appLockEnabled = Settings.app_lock_enabled.field.collectAsStateWithLifecycle().value
    var passwordSet by remember { mutableStateOf(AppLockRepository.isPasswordSet(context)) }
    var dialogMode by remember { mutableStateOf<AppLockDialogMode?>(null) }
    val enabled = appLockEnabled && passwordSet

    LaunchedEffect(appLockEnabled, passwordSet) {
        if (appLockEnabled && !passwordSet) {
            Settings.app_lock_enabled.setValue(false)
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
    }
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
                        onDone = { if (canSubmit) submit() }
                    )
                    PasswordSettingField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorText = null
                        },
                        label = "Повтор кода доступа",
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
    onDone: () -> Unit
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
            keyboardType = KeyboardType.Text,
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
