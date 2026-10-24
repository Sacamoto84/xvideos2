package com.client.xvideos.x.screens.login.molecule

import android.view.autofill.AutofillManager
import android.webkit.WebView
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.applock.atom.SecurePasswordToolbar
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.AllowWindowAutofill
import com.client.xvideos.common.ui.IncognitoKeyboard
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.adaptive.capOutsideCompactDp
import com.client.xvideos.common.ui.adaptive.currentWidthClass
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.feature.auth.XSignInStep
import com.client.xvideos.x.screens.login.atom.XSignInProgress
import com.client.xvideos.x.screens.login.atom.XSignInWebViewHost
import com.client.xvideos.x.urlStart
import timber.log.Timber

private val ACCENT_X = Color(0xFFDE2600)
private val BUTTON_BACK = Color(0xFF1E88E5)
private val TEXT_COLOR = Theme.Text.secondary
private val FIELD_TEXT_STYLE = TextStyle(fontSize = 24.sp)
private const val LOGIN_FORM_MAX_WIDTH_DP = 420

/**
 * Форма входа в профиль X: логин, пароль, «Войти», «Назад» — как форма L.
 *
 * В отличие от L, «Войти» сразу входит на сайт, а профиль сохраняется, только
 * когда сайт его принял. Вход идёт через страницу сайта в WebView: там сайт
 * решает свою капчу, поэтому вход может занять заметное время, — форма
 * показывает этап и секунды ([XSignInProgress]).
 *
 * Набранные логин и пароль и ход входа форма не хранит: их держит экран
 * (`XLoginFormState` в его ScreenModel).
 *
 * @param step этап идущего входа; `null` — вход не идёт.
 * @param error почему последний вход не удался; `null` — ошибки нет.
 * @param signedIn сайт принял вход: форма завершает автозаполнение и зовёт [onSignedIn].
 * @param signInWebView WebView идущего входа — форма держит его в окне, см. [XSignInWebViewHost].
 * @param modifier применяется к корню формы, до её фона и отступов.
 */
@Suppress("LongMethod")
@Composable
fun XLoginForm(
    login: String,
    password: String,
    step: XSignInStep?,
    error: String?,
    signedIn: Boolean,
    signInWebView: WebView?,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Как у формы L: поля объявлены логином и паролем для менеджера паролей,
    // удачный вход завершает сеанс автозаполнения.
    AllowWindowAutofill()

    var passwordVisible by remember { mutableStateOf(false) }
    val isSigningIn = step != null

    val context = LocalContext.current
    val autofillManager = remember(context) { context.getSystemService(AutofillManager::class.java) }
    val uriHandler = LocalUriHandler.current
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)

    LaunchedEffect(signedIn) {
        if (signedIn) {
            autofillManager?.commit()
            SnackBar.success("Вход в X выполнен")
            currentOnSignedIn()
        }
    }

    val onTogglePasswordVisible = remember { { passwordVisible = !passwordVisible } }

    val onOpenWebsite: () -> Unit = remember(uriHandler) {
        {
            runCatching {
                uriHandler.openUri(urlStart)
            }.onFailure { e ->
                Timber.w("XLoginForm: не удалось открыть ссылку X: ${e.javaClass.simpleName}")
                SnackBar.error("Не удалось открыть ссылку")
            }.let {}
        }
    }

    val keyboardActions = remember(onSignIn) {
        KeyboardActions(onDone = { onSignIn() })
    }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF484848),
        unfocusedContainerColor = Color(0xFF3A3A3A),
        disabledContainerColor = Color(0xFF303030),
        focusedTextColor = TEXT_COLOR,
        unfocusedTextColor = TEXT_COLOR,
        disabledTextColor = Theme.Text.muted,
        focusedIndicatorColor = Color(0xFF888888),
        cursorColor = ACCENT_X,
    )

    val passwordTrailingIcon: @Composable () -> Unit = remember(passwordVisible) {
        {
            IconButton(onClick = onTogglePasswordVisible) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                    tint = TEXT_COLOR
                )
            }
        }
    }

    val scrollState = rememberScrollState()
    // Как у формы L: с открытой клавиатурой содержимое начинается от верхнего
    // края, и без отступа первая строка уходила под статус-бар.
    val topInset = getTopInsetDp()

    // На широком окне форма не шире 420dp по центру; фон и прокрутка — на всю
    // ширину. На телефоне null: разметка как у формы L.
    val formCap = capOutsideCompactDp(currentWidthClass(), LOGIN_FORM_MAX_WIDTH_DP)

    Column(
        modifier = modifier
            .background(Color(0xFF212121))
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .then(formCap?.let { Modifier.adaptiveContentWidth(it.dp) } ?: Modifier)
            .padding(top = topInset)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        XSignInWebViewHost(webView = signInWebView)

        Text(
            text = urlStart,
            fontStyle = FontStyle.Italic,
            textDecoration = TextDecoration.Underline,
            color = ACCENT_X,
            modifier = Modifier.clickable(onClick = onOpenWebsite),
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Авторизация",
            style = MaterialTheme.typography.headlineMedium,
            color = TEXT_COLOR
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Логин (email)",
            color = TEXT_COLOR,
            fontSize = 22.sp,
            fontFamily = Theme.L.fontFamilyApp
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = login,
            onValueChange = onLoginChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Username },
            enabled = !isSigningIn,
            singleLine = true,
            keyboardOptions = IncognitoKeyboard.options(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            colors = textFieldColors,
            textStyle = FIELD_TEXT_STYLE
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Пароль",
            color = TEXT_COLOR,
            fontSize = 22.sp,
            fontFamily = Theme.L.fontFamilyApp
        )

        Spacer(modifier = Modifier.height(8.dp))

        SecurePasswordToolbar {
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Password },
                enabled = !isSigningIn,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = passwordTrailingIcon,
                keyboardOptions = IncognitoKeyboard.options(
                    forceIncognito = true,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = keyboardActions,
                singleLine = true,
                colors = textFieldColors,
                textStyle = FIELD_TEXT_STYLE
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider(color = Color.DarkGray)

        Spacer(modifier = Modifier.height(32.dp))

        val buttonShape = RoundedCornerShape(8.dp)

        Button(
            onClick = onSignIn,
            enabled = !isSigningIn,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ACCENT_X,
                disabledContainerColor = ACCENT_X.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.7f),
            ),
            shape = buttonShape
        ) {
            Text(
                "Войти",
                fontSize = 22.sp,
                fontFamily = Theme.L.fontFamilyApp
            )
        }

        if (step != null) {
            XSignInProgress(
                step = step,
                color = TEXT_COLOR,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else if (error != null) {
            Text(
                text = error,
                color = Theme.Feedback.error,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
            )
        }

        Text(
            text = "Вход идёт через страницу сайта: она сама проверяет устройство, это может занять время.",
            color = Theme.Text.muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BUTTON_BACK),
            shape = buttonShape
        ) {
            Text(
                "Назад",
                fontSize = 22.sp,
                fontFamily = Theme.L.fontFamilyApp
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
private fun XLoginFormPreview() {
    XLoginForm(
        login = "preview@example.com",
        password = "",
        step = null,
        error = null,
        signedIn = false,
        signInWebView = null,
        onLoginChange = {},
        onPasswordChange = {},
        onSignIn = {},
        onSignedIn = {},
        onBack = {}
    )
}

@Preview(showBackground = false)
@Composable
private fun XLoginFormSigningInPreview() {
    XLoginForm(
        login = "preview@example.com",
        password = "secret",
        step = XSignInStep.CAPTCHA,
        error = null,
        signedIn = false,
        signInWebView = null,
        onLoginChange = {},
        onPasswordChange = {},
        onSignIn = {},
        onSignedIn = {},
        onBack = {}
    )
}

@Preview(showBackground = false)
@Composable
private fun XLoginFormErrorPreview() {
    XLoginForm(
        login = "preview@example.com",
        password = "secret",
        step = null,
        error = "Не удается войти. Пожалуйста, проверьте свой логин/пароль.",
        signedIn = false,
        signInWebView = null,
        onLoginChange = {},
        onPasswordChange = {},
        onSignIn = {},
        onSignedIn = {},
        onBack = {}
    )
}
