package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.applock.DisableAppLockAutofill
import com.client.xvideos.common.applock.atom.SecurePasswordToolbar
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.IncognitoKeyboard
import com.client.xvideos.common.ui.excludedFromAutofill

/**
 * Диалог ввода пароля для расшифровки и чтения зашифрованного бэкапа.
 */
@Composable
internal fun BackupRestorePasswordDialog(
    errorMessage: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (password: CharArray) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isValid = password.isNotEmpty() && !busy

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

            SecurePasswordToolbar {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    enabled = !busy,
                    label = { Text(TEXT_PASSWORD_LABEL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().excludedFromAutofill(),
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
            }

            if (busy) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = dialogTheme.bodyColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Проверка пароля и чтение архива…",
                        style = Theme.L.Type.dialogBody.copy(color = dialogTheme.bodyColor)
                    )
                }
            } else if (!errorMessage.isNullOrBlank()) {
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

@Preview
@Composable
private fun BackupRestorePasswordDialogPreview() {
    BackupRestorePasswordDialog(errorMessage = "Неверный пароль архива", busy = false, onDismiss = {}, onConfirm = {})
}
