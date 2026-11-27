package com.client.xvideos.screenSettings.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch

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
    var isSubmitting by remember { mutableStateOf(false) }
    val currentOnComplete by rememberUpdatedState(onComplete)
    val scope = rememberCoroutineScope()

    val isCamouflage by Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle()
    val passwordKeyboardType = if (isCamouflage) KeyboardType.NumberPassword else KeyboardType.Text

    val needsCurrentPassword = mode == AppLockDialogMode.CHANGE || mode == AppLockDialogMode.DISABLE
    val needsNewPassword = mode == AppLockDialogMode.SET || mode == AppLockDialogMode.CHANGE
    val canSubmit = canSubmitAppLockPassword(mode, currentPassword, newPassword, confirmPassword)

    fun submit() {
        if (isSubmitting) return
        errorText = null
        scope.launch {
            isSubmitting = true
            try {
                errorText = executeAppLockPasswordAction(
                    context = context,
                    mode = mode,
                    isCamouflage = isCamouflage,
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    onSuccess = currentOnComplete,
                )
            } finally {
                isSubmitting = false
            }
        }
    }

    val currentSubmit by rememberUpdatedState(::submit)
    val currentCanSubmit by rememberUpdatedState(canSubmit)
    val onCurrentPasswordChange = remember { { value: String -> currentPassword = value; errorText = null } }
    val onNewPasswordChange = remember { { value: String -> newPassword = value; errorText = null } }
    val onConfirmPasswordChange = remember { { value: String -> confirmPassword = value; errorText = null } }
    val onDoneSubmit = remember { { if (currentCanSubmit) currentSubmit() } }
    val onConfirmSubmit = remember { { currentSubmit() } }

    LavenderDialog(
        title = appLockPasswordDialogTitle(mode),
        onDismiss = onDismiss,
        content = {
            DisableAppLockAutofill()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Text(err, color = appLockErrorColor, style = ERROR_TEXT_STYLE)
                }
            }
        },
        confirmText = if (mode == AppLockDialogMode.DISABLE) "Отключить" else "Сохранить",
        onConfirm = onConfirmSubmit,
        confirmEnabled = canSubmit,
        destructive = mode == AppLockDialogMode.DISABLE,
    )
}

private fun appLockPasswordDialogTitle(mode: AppLockDialogMode): String = when (mode) {
    AppLockDialogMode.SET -> "Задать код доступа"
    AppLockDialogMode.CHANGE -> "Изменить код доступа"
    AppLockDialogMode.DISABLE -> "Отключить код доступа"
}

private fun canSubmitAppLockPassword(
    mode: AppLockDialogMode,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
): Boolean = when (mode) {
    AppLockDialogMode.SET -> newPassword.length >= 4 && confirmPassword.isNotBlank()
    AppLockDialogMode.CHANGE ->
        currentPassword.isNotBlank() && newPassword.length >= 4 && confirmPassword.isNotBlank()
    AppLockDialogMode.DISABLE -> currentPassword.isNotBlank()
}

private suspend fun executeAppLockPasswordAction(
    context: Context,
    mode: AppLockDialogMode,
    isCamouflage: Boolean,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    onSuccess: () -> Unit,
): String? {
    val needsCurrentPassword = mode == AppLockDialogMode.CHANGE || mode == AppLockDialogMode.DISABLE
    val needsNewPassword = mode == AppLockDialogMode.SET || mode == AppLockDialogMode.CHANGE

    if (needsCurrentPassword) {
        when (val check = AppLockRepository.verifyPasswordThrottled(context, currentPassword)) {
            is AppLockCheckResult.LockedOut -> return lockedOutMessage(check.remainingMs)
            is AppLockCheckResult.Wrong -> return "Текущий код доступа не подходит"
            AppLockCheckResult.Success -> Unit
        }
    }
    if (needsNewPassword && newPassword != confirmPassword) {
        return "Коды доступа не совпадают"
    }
    if (isCamouflage && needsNewPassword && !CalculatorState.isEnterableCode(newPassword)) {
        return "При включённой маскировке код доступа — только цифры, не длиннее ${CalculatorState.MAX_CODE_DIGITS}"
    }
    if (needsNewPassword && AppLockRepository.isDuressCodeSet(context) &&
        AppLockRepository.verifyDuressCode(context, newPassword)
    ) {
        return "Код доступа не может совпадать с кодом уничтожения"
    }

    return when (mode) {
        AppLockDialogMode.SET -> saveNewPassword(context, newPassword, "Код доступа включён", "Не удалось сохранить код доступа", onSuccess)
        AppLockDialogMode.CHANGE -> saveNewPassword(context, newPassword, "Код доступа изменён", "Не удалось изменить код доступа", onSuccess)
        AppLockDialogMode.DISABLE -> {
            AppLockRepository.clearPassword(context)
            SnackBar.success("Код доступа отключён")
            onSuccess()
            null
        }
    }
}

private suspend fun saveNewPassword(
    context: Context,
    newPassword: String,
    successMessage: String,
    fallbackError: String,
    onSuccess: () -> Unit,
): String? {
    var errorMessage: String? = null
    AppLockRepository.setPassword(context, newPassword)
        .onSuccess {
            SnackBar.success(successMessage)
            onSuccess()
        }
        .onFailure { error ->
            errorMessage = error.message ?: fallbackError
        }
    return errorMessage
}

@Preview
@Composable
private fun AppLockPasswordDialogPreview() {
    AppLockPasswordDialog(mode = AppLockDialogMode.SET, onDismiss = {}, onComplete = {})
}
