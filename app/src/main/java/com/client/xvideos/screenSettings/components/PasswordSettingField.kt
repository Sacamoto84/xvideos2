package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.applock.AccessCodeVisualTransformation
import com.client.xvideos.common.applock.DisableAppLockAutofill
import com.client.xvideos.common.applock.atom.SecurePasswordToolbar
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.IncognitoKeyboard
import com.client.xvideos.common.ui.excludedFromAutofill

/**
 * Поле ввода кода доступа в диалогах настроек.
 */
@Composable
fun PasswordSettingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    DisableAppLockAutofill()

    var showPassword by remember { mutableStateOf(false) }
    val dialogTheme = Theme.DialogLavande
    val onToggleShowPassword = remember { { showPassword = !showPassword } }
    val keyboardOptions = remember(keyboardType) {
        IncognitoKeyboard.options(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
            forceIncognito = true
        )
    }
    val keyboardActions = remember(onDone) {
        KeyboardActions(onDone = { onDone() })
    }

    val labelComposable: @Composable () -> Unit = remember(label) { { Text(label) } }
    val trailingIconComposable: @Composable () -> Unit =
        remember(showPassword, dialogTheme.dismissTextColor, onToggleShowPassword) {
            {
                IconButton(onClick = onToggleShowPassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPassword) "Скрыть код доступа" else "Показать код доступа",
                        tint = dialogTheme.dismissTextColor
                    )
                }
            }
        }
    val fieldModifier = modifier.fillMaxWidth().excludedFromAutofill()
    val fieldTextStyle = remember(dialogTheme.bodyColor) {
        Theme.L.Type.body.copy(color = dialogTheme.bodyColor)
    }

    SecurePasswordToolbar {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier,
            label = labelComposable,
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else AccessCodeVisualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = fieldTextStyle,
            trailingIcon = trailingIconComposable,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = dialogTheme.bodyColor,
                unfocusedTextColor = dialogTheme.bodyColor,
                cursorColor = dialogTheme.dismissTextColor,
                focusedBorderColor = dialogTheme.dismissTextColor,
                unfocusedBorderColor = Color(0xFF9A9A9A),
                focusedLabelColor = dialogTheme.dismissTextColor,
                unfocusedLabelColor = Color(0xFF6E6E6E)
            )
        )
    }
}

@Preview
@Composable
private fun PasswordSettingFieldPreview() {
    PasswordSettingField(
        value = "1234",
        onValueChange = {},
        label = "Код доступа",
        onDone = {}
    )
}
