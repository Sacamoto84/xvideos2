package com.client.xvideos.l.ui.screens

import android.view.autofill.AutofillManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.theme.ThemeL

class L_ScreenLogin : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        LLoginContent(
            initialLogin = Settings.l_login.field.value,
            initialPassword = Settings.l_pass.field.value,
            onSaved = { navigator.pop() },
            onBack = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLoginContent(
    initialLogin: String = "",
    initialPassword: String = "",
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    var login by remember(initialLogin) { mutableStateOf(initialLogin) }
    var password by remember(initialPassword) { mutableStateOf(initialPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val autofillManager = remember(context) { context.getSystemService(AutofillManager::class.java) }
    val uriHandler = LocalUriHandler.current

    fun saveCredentials() {
        val normalizedLogin = login.trim()
        if (normalizedLogin.isBlank() || password.isBlank()) {
            SnackBar.warning("Введите логин и пароль L")
            return
        }
        Settings.l_login.setValue(normalizedLogin)
        Settings.l_pass.setValue(password)
        autofillManager?.commit()
        SnackBar.success("Авторизация L сохранена")
        onSaved()
    }

    Column(
        modifier = Modifier
            .background(Color(0xFF212121))
            .padding(horizontal = 16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "https://www.luscious.net",
            fontStyle = FontStyle.Italic,
            textDecoration = TextDecoration.Underline,
            color = ThemeL.b0,
            modifier = Modifier.clickable { uriHandler.openUri("https://www.luscious.net") },
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Авторизация",
            style = MaterialTheme.typography.headlineMedium,
            color = ThemeL.textColor
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Логин",
            color = ThemeL.textColor,
            fontSize = 22.sp,
            fontFamily = ThemeL.fontFamilyKarla
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Username },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF484848),
                unfocusedContainerColor = Color(0xFF3A3A3A),
                focusedTextColor = Color(0xFFB8B7B7),
                unfocusedTextColor = Color(0xFFB8B7B7),
                focusedIndicatorColor = Color(0xFF888888),
            ),
            textStyle = TextStyle(
                fontSize = 24.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Пароль",
            color = ThemeL.textColor,
            fontSize = 22.sp,
            fontFamily = ThemeL.fontFamilyKarla
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFFB8B7B7)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { saveCredentials() }),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF484848),
                unfocusedContainerColor = Color(0xFF3A3A3A),
                focusedTextColor = Color(0xFFB8B7B7),
                unfocusedTextColor = Color(0xFFB8B7B7),
                focusedIndicatorColor = Color(0xFF888888),
            ),
            textStyle = TextStyle(
                fontSize = 24.sp
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider(color = Color.DarkGray)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { saveCredentials() },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ThemeL.primaryColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Сохранить",
                fontSize = 22.sp,
                fontFamily = ThemeL.fontFamilyKarla
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ThemeL.b0),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Назад",
                fontSize = 22.sp,
                fontFamily = ThemeL.fontFamilyKarla
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun LLoginContentPreview() {
    LLoginContent(
        onSaved = {},
        onBack = {}
    )
}
