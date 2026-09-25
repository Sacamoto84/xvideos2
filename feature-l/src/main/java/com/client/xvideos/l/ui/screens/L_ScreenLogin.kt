package com.client.xvideos.l.ui.screens

import com.client.xvideos.common.theme.Theme

import android.view.autofill.AutofillManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import timber.log.Timber

private val BACKGROUND_COLOR = Color(0xFF212121)
private val CONTAINER_FOCUSED_COLOR = Color(0xFF484848)
private val CONTAINER_UNFOCUSED_COLOR = Color(0xFF3A3A3A)
private val TEXT_COLOR_MUTED = Color(0xFFB8B7B7)
private val INDICATOR_COLOR = Color(0xFF888888)
private val DIVIDER_COLOR = Color.DarkGray

private val ICON_PASSWORD_VISIBLE = Icons.Default.VisibilityOff
private val ICON_PASSWORD_HIDDEN = Icons.Default.Visibility

private val BUTTON_CORNER_RADIUS = 8.dp
private val BUTTON_SHAPE = RoundedCornerShape(BUTTON_CORNER_RADIUS)

private val FIELD_TEXT_STYLE = TextStyle(fontSize = 24.sp)

private val KEYBOARD_OPTIONS_EMAIL = KeyboardOptions(
    keyboardType = KeyboardType.Email,
    imeAction = ImeAction.Next
)
private val KEYBOARD_OPTIONS_PASSWORD = KeyboardOptions(
    keyboardType = KeyboardType.Password,
    imeAction = ImeAction.Done
)

private val COLUMN_HORIZONTAL_ALIGNMENT = Alignment.CenterHorizontally
private val COLUMN_VERTICAL_ARRANGEMENT = Arrangement.Center

private val BUTTON_BASE_MODIFIER = Modifier.fillMaxWidth().height(64.dp)
private val SKIP_BUTTON_MODIFIER = Modifier.padding(top = 24.dp).fillMaxWidth().height(64.dp)

private val SPACER_HEIGHT_8_MODIFIER = Modifier.height(8.dp)
private val SPACER_HEIGHT_16_MODIFIER = Modifier.height(16.dp)
private val SPACER_HEIGHT_32_MODIFIER = Modifier.height(32.dp)

private const val URL_LUSCIOUS = "https://www.luscious.net"
private const val DESC_SHOW_PASSWORD = "Показать пароль"
private const val DESC_HIDE_PASSWORD = "Скрыть пароль"

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLoginContent(
    initialLogin: String = "",
    initialPassword: String = "",
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit

) {
    var login by remember(initialLogin) { mutableStateOf(initialLogin) }
    var password by remember(initialPassword) { mutableStateOf(initialPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val autofillManager = remember(context) { context.getSystemService(AutofillManager::class.java) }
    val uriHandler = LocalUriHandler.current

    val onLoginChange = remember { { newLogin: String -> login = newLogin } }
    val onPasswordChange = remember { { newPassword: String -> password = newPassword } }
    val onTogglePasswordVisible = remember { { passwordVisible = !passwordVisible } }

    val onSaveCredentials = remember(login, password, autofillManager, onSaved) {
        {
            val normalizedLogin = login.trim()
            if (normalizedLogin.isBlank() || password.isBlank()) {
                SnackBar.warning("Введите логин и пароль L")
            } else {
                Settings.l_login.setValue(normalizedLogin)
                Settings.l_pass.setValue(password)
                autofillManager?.commit()
                SnackBar.success("Авторизация L сохранена")
                onSaved()
            }
        }
    }

    val onOpenWebsite: () -> Unit = remember(uriHandler) {
        {
            runCatching {
                uriHandler.openUri(URL_LUSCIOUS)
            }.onFailure { e ->
                Timber.w(e, "L_ScreenLogin: не удалось открыть ссылку Luscious")
                SnackBar.error("Не удалось открыть ссылку")
            }.let {}
        }
    }

    val keyboardActions = remember(onSaveCredentials) {
        KeyboardActions(onDone = { onSaveCredentials() })
    }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = CONTAINER_FOCUSED_COLOR,
        unfocusedContainerColor = CONTAINER_UNFOCUSED_COLOR,
        focusedTextColor = TEXT_COLOR_MUTED,
        unfocusedTextColor = TEXT_COLOR_MUTED,
        focusedIndicatorColor = INDICATOR_COLOR,
    )

    val passwordTrailingIcon: @Composable () -> Unit = remember(passwordVisible) {
        {
            IconButton(onClick = onTogglePasswordVisible) {
                Icon(
                    imageVector = if (passwordVisible) ICON_PASSWORD_VISIBLE else ICON_PASSWORD_HIDDEN,
                    contentDescription = if (passwordVisible) DESC_HIDE_PASSWORD else DESC_SHOW_PASSWORD,
                    tint = TEXT_COLOR_MUTED
                )
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .background(BACKGROUND_COLOR)
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = COLUMN_HORIZONTAL_ALIGNMENT,
        verticalArrangement = COLUMN_VERTICAL_ARRANGEMENT
    ) {

        Text(
            text = URL_LUSCIOUS,
            fontStyle = FontStyle.Italic,
            textDecoration = TextDecoration.Underline,
            color = Theme.L.b0,
            modifier = Modifier.clickable(onClick = onOpenWebsite),
            fontSize = 24.sp
        )

        Spacer(modifier = SPACER_HEIGHT_32_MODIFIER)

        Text(
            text = "Авторизация",
            style = MaterialTheme.typography.headlineMedium,
            color = Theme.L.textColor
        )

        Spacer(modifier = SPACER_HEIGHT_32_MODIFIER)

        Text(
            "Логин",
            color = Theme.L.textColor,
            fontSize = 22.sp,
            fontFamily = Theme.L.fontFamilyKarla
        )

        Spacer(modifier = SPACER_HEIGHT_8_MODIFIER)

        OutlinedTextField(
            value = login,
            onValueChange = onLoginChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Username },
            singleLine = true,
            keyboardOptions = KEYBOARD_OPTIONS_EMAIL,
            colors = textFieldColors,
            textStyle = FIELD_TEXT_STYLE
        )

        Spacer(modifier = SPACER_HEIGHT_16_MODIFIER)

        Text(
            "Пароль",
            color = Theme.L.textColor,
            fontSize = 22.sp,
            fontFamily = Theme.L.fontFamilyKarla
        )

        Spacer(modifier = SPACER_HEIGHT_8_MODIFIER)

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = passwordTrailingIcon,
            keyboardOptions = KEYBOARD_OPTIONS_PASSWORD,
            keyboardActions = keyboardActions,
            singleLine = true,
            colors = textFieldColors,
            textStyle = FIELD_TEXT_STYLE
        )

        Spacer(modifier = SPACER_HEIGHT_32_MODIFIER)

        HorizontalDivider(color = DIVIDER_COLOR)

        Spacer(modifier = SPACER_HEIGHT_32_MODIFIER)

        Button(
            onClick = onSaveCredentials,
            modifier = BUTTON_BASE_MODIFIER,
            colors = ButtonDefaults.buttonColors(containerColor = Theme.L.primaryColor),
            shape = BUTTON_SHAPE
        ) {
            Text(
                "Сохранить",
                fontSize = 22.sp,
                fontFamily = Theme.L.fontFamilyKarla
            )
        }
        Spacer(modifier = SPACER_HEIGHT_32_MODIFIER)

        Button(
            onClick = onBack,
            modifier = BUTTON_BASE_MODIFIER,
            colors = ButtonDefaults.buttonColors(containerColor = Theme.L.b0),
            shape = BUTTON_SHAPE
        ) {
            Text(
                "Назад",
                fontSize = 22.sp,
                fontFamily = Theme.L.fontFamilyKarla
            )
        }

        TextButton(
            onClick = onSkip,
            modifier = SKIP_BUTTON_MODIFIER,
            shape = BUTTON_SHAPE
        ) {
            Text(
                text = "Пропустить",
                fontSize = 22.sp,
                fontFamily = Theme.L.fontFamilyKarla,
                color = Theme.L.b0,
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun LLoginContentPreview() {
    LLoginContent(
        onSaved = {},
        onBack = {},
        onSkip = {}
    )
}
