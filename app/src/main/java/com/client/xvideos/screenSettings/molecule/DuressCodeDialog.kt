package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.calculator.CalculatorState
import com.client.xvideos.common.applock.AppLockCheckResult
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.DisableAppLockAutofill
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.screenSettings.components.PasswordSettingField
import com.client.xvideos.screenSettings.components.SettingsPreview
import kotlinx.coroutines.launch
import kotlin.math.ceil

private val duressErrorColor = Color(0xFFB3261E)

private fun lockedOutMessage(remainingMs: Long): String =
    "Слишком много попыток. Повторите через ${ceil(remainingMs / 1000.0).toInt()} с"

/**
 * Диалог задания, изменения и удаления кода уничтожения.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun DuressCodeDialog(
    mode: DuressDialogMode,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
) {
    DisableAppLockAutofill()

    val context = LocalContext.current.applicationContext
    var currentCode by remember { mutableStateOf("") }
    var newCode by remember { mutableStateOf("") }
    var confirmCode by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isCamouflage by Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle()
    val passwordKeyboardType = if (isCamouflage) KeyboardType.NumberPassword else KeyboardType.Text

    val needsCurrentCode = mode == DuressDialogMode.CHANGE || mode == DuressDialogMode.REMOVE
    val needsNewCode = mode == DuressDialogMode.SET || mode == DuressDialogMode.CHANGE
    val canSubmit = when (mode) {
        DuressDialogMode.SET -> newCode.length >= 4 && confirmCode.isNotBlank()
        DuressDialogMode.CHANGE -> currentCode.isNotBlank() && newCode.length >= 4 && confirmCode.isNotBlank()
        DuressDialogMode.REMOVE -> currentCode.isNotBlank()
    }

    fun submit() {
        if (isSubmitting) return
        errorText = null

        scope.launch {
            isSubmitting = true
            try {
                if (needsCurrentCode) {
                    val check = AppLockRepository.verifyDuressCodeThrottled(context, currentCode)
                    if (check is AppLockCheckResult.LockedOut) {
                        errorText = lockedOutMessage(check.remainingMs)
                        return@launch
                    }
                    if (check is AppLockCheckResult.Wrong) {
                        errorText = "Текущий код уничтожения не подходит"
                        return@launch
                    }
                }

                if (needsNewCode) {
                    if (newCode != confirmCode) {
                        errorText = "Коды уничтожения не совпадают"
                        return@launch
                    }

                    if (isCamouflage && !CalculatorState.isEnterableCode(newCode)) {
                        errorText = "При включённой маскировке код — только цифры, не длиннее " +
                            "${CalculatorState.MAX_CODE_DIGITS}"
                        return@launch
                    }

                    if (AppLockRepository.verifyPassword(context, newCode)) {
                        errorText = "Код уничтожения не может совпадать с кодом доступа"
                        return@launch
                    }
                }

                when (mode) {
                    DuressDialogMode.SET -> {
                        AppLockRepository.setDuressCode(context, newCode).onSuccess {
                            SnackBar.success("Код уничтожения задан")
                            onComplete()
                        }.onFailure { error ->
                            errorText = error.message ?: "Не удалось сохранить код уничтожения"
                        }
                    }
                    DuressDialogMode.CHANGE -> {
                        AppLockRepository.setDuressCode(context, newCode).onSuccess {
                            SnackBar.success("Код уничтожения изменён")
                            onComplete()
                        }.onFailure { error ->
                            errorText = error.message ?: "Не удалось изменить код уничтожения"
                        }
                    }
                    DuressDialogMode.REMOVE -> {
                        AppLockRepository.clearDuressCode(context)
                        SnackBar.success("Код уничтожения удалён")
                        onComplete()
                    }
                }
            } finally {
                isSubmitting = false
            }
        }
    }

    val onCurrentCodeChange = remember {
        { value: String ->
            currentCode = value
            errorText = null
        }
    }
    val onNewCodeChange = remember {
        { value: String ->
            newCode = value
            errorText = null
        }
    }
    val onConfirmCodeChange = remember {
        { value: String ->
            confirmCode = value
            errorText = null
        }
    }
    val onDoneSubmit = remember(canSubmit) {
        {
            if (canSubmit) submit()
        }
    }
    val onConfirmSubmit = remember { { submit() } }

    val errorTextStyle = remember {
        Theme.L.Type.dialogBody.copy(color = duressErrorColor)
    }

    LavenderDialog(
        title = when (mode) {
            DuressDialogMode.SET -> "Задать код уничтожения"
            DuressDialogMode.CHANGE -> "Изменить код уничтожения"
            DuressDialogMode.REMOVE -> "Убрать код уничтожения"
        },
        onDismiss = onDismiss,
        content = {
            DisableAppLockAutofill()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (needsCurrentCode) {
                    PasswordSettingField(
                        value = currentCode,
                        onValueChange = onCurrentCodeChange,
                        label = "Текущий код уничтожения",
                        keyboardType = passwordKeyboardType,
                        onDone = onDoneSubmit,
                    )
                }

                if (needsNewCode) {
                    PasswordSettingField(
                        value = newCode,
                        onValueChange = onNewCodeChange,
                        label = "Новый код уничтожения",
                        keyboardType = passwordKeyboardType,
                        onDone = onDoneSubmit,
                    )
                    PasswordSettingField(
                        value = confirmCode,
                        onValueChange = onConfirmCodeChange,
                        label = "Повтор кода уничтожения",
                        keyboardType = passwordKeyboardType,
                        onDone = onDoneSubmit,
                    )
                }

                errorText?.let { err ->
                    Text(err, color = duressErrorColor, style = errorTextStyle)
                }
            }
        },
        confirmText = when (mode) {
            DuressDialogMode.REMOVE -> "Убрать"
            else -> "Сохранить"
        },
        onConfirm = onConfirmSubmit,
        confirmEnabled = canSubmit && !isSubmitting,
        destructive = mode == DuressDialogMode.REMOVE,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun DuressCodeDialogPreview() = SettingsPreview {
    DuressCodeDialog(
        mode = DuressDialogMode.SET,
        onDismiss = {},
        onComplete = {},
    )
}
