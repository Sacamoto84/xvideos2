package com.client.xvideos.common.applock.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.applock.AccessCodeVisualTransformation
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun AppLockInputForm(
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordToggle: () -> Unit,
    errorText: String?,
    isLockedOut: Boolean,
    lockoutSeconds: Int,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            enabled = !isLockedOut,
            label = { Text("Код доступа") },
            isError = errorText != null || isLockedOut,
            visualTransformation = if (showPassword) VisualTransformation.None else AccessCodeVisualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (password.isNotBlank() && !isLockedOut) onSubmit() }),
            textStyle = Theme.L.Type.body.copy(color = MaterialTheme.colorScheme.onSurface),
            trailingIcon = {
                IconButton(onClick = onShowPasswordToggle) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPassword) "Скрыть код доступа" else "Показать код доступа",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = appLockTextFieldColors(),
            supportingText = {
                if (errorText != null || isLockedOut) {
                    Text(
                        text = if (isLockedOut) "Повторите через $lockoutSeconds с" else (errorText ?: ""),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onSubmit,
            enabled = password.isNotBlank() && !isLockedOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        ) {
            Text(
                text = if (isLockedOut) "Подождите $lockoutSeconds с" else "Разблокировать",
                style = Theme.L.Type.rowTitle.copy(
                    color = if (password.isNotBlank() && !isLockedOut) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun appLockTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error,
    errorCursorColor = MaterialTheme.colorScheme.error,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    errorTrailingIconColor = MaterialTheme.colorScheme.primary,
)

@Preview
@Composable
private fun AppLockInputFormPreview() {
    XvideosTheme(darkTheme = true) {
        AppLockInputForm(
            password = "123",
            onPasswordChange = {},
            showPassword = false,
            onShowPasswordToggle = {},
            errorText = null,
            isLockedOut = false,
            lockoutSeconds = 0,
            onSubmit = {},
            focusRequester = FocusRequester()
        )
    }
}
