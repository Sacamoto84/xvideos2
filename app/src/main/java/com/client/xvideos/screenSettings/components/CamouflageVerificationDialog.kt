package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.calculator.CalculatorState
import com.client.xvideos.calculator.LauncherAliasManager
import com.client.xvideos.common.applock.AppLockCheckResult
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.DisableAppLockAutofill
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import kotlinx.coroutines.launch

@Composable
internal fun CamouflageVerificationDialog(
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
                if (!CalculatorState.isEnterableCode(pinInput)) {
                    pinError = "Код доступа для калькулятора — только цифры, не длиннее " +
                        "${CalculatorState.MAX_CODE_DIGITS}"
                } else {
                    scope.launch {
                        isVerifyingPin = true
                        try {
                            when (val check = AppLockRepository.verifyPasswordThrottled(context, pinInput)) {
                                AppLockCheckResult.Success -> {
                                    Settings.camouflage_calculator_enabled.setValue(true)
                                    LauncherAliasManager.setCalculatorAliasEnabled(context, true)
                                    pinInput = ""
                                    onSuccess()
                                    SnackBar.success("Маскировка под калькулятор включена")
                                }
                                is AppLockCheckResult.Wrong ->
                                    pinError = "Неверный код доступа. Если текущий пароль содержит буквы, сначала измените его на числовой PIN."
                                is AppLockCheckResult.LockedOut ->
                                    pinError = lockedOutMessage(check.remainingMs)
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Text(err, color = appLockErrorColor, style = ERROR_TEXT_STYLE)
                }
            }
        },
        confirmText = "Включить",
        confirmEnabled = pinInput.length >= 4 && !isVerifyingPin,
        onConfirm = onConfirmVerification
    )
}

@Preview
@Composable
private fun CamouflageVerificationDialogPreview() {
    CamouflageVerificationDialog(onDismiss = {}, onSuccess = {})
}
