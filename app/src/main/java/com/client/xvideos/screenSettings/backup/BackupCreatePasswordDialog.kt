package com.client.xvideos.screenSettings.backup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
internal fun BackupCreatePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: CharArray) -> Unit,
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

            SecurePasswordToolbar {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
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
                        imeAction = ImeAction.Next
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SecurePasswordToolbar {
                OutlinedTextField(
                    value = passwordConfirm,
                    onValueChange = { passwordConfirm = it },
                    label = { Text("Подтверждение пароля") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().excludedFromAutofill(),
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
            }

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

@Preview
@Composable
private fun BackupCreatePasswordDialogPreview() {
    BackupCreatePasswordDialog(onDismiss = {}, onConfirm = {})
}
