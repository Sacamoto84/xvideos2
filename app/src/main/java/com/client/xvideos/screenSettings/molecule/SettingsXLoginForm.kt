package com.client.xvideos.screenSettings.molecule

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.settings.element.SavedCredentials
import com.client.xvideos.x.feature.auth.XSignInResult
import com.client.xvideos.x.screens.login.XLoginFormState
import com.client.xvideos.x.screens.login.molecule.XLoginForm

/**
 * Форма входа X на месте страницы настроек: разворачивает состояние формы в
 * параметры [XLoginForm]. «Назад» и удачный вход одинаково зовут [onClose] —
 * экран прячет форму и стирает набранное.
 *
 * @param form состояние формы из `AppSettingsSM`.
 * @param signInWebView WebView идущего входа, см. `XSignInWebViewHost`.
 */
@Composable
internal fun SettingsXLoginForm(
    form: XLoginFormState,
    signInWebView: WebView?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onLoginChange = remember(form) { form::updateLogin }
    val onPasswordChange = remember(form) { form::updatePassword }
    val onSignIn = remember(form) { form::submit }

    XLoginForm(
        login = form.login,
        password = form.password,
        step = form.step,
        error = form.error,
        signedIn = form.signedIn,
        signInWebView = signInWebView,
        onLoginChange = onLoginChange,
        onPasswordChange = onPasswordChange,
        onSignIn = onSignIn,
        onSignedIn = onClose,
        onBack = onClose,
        modifier = modifier
    )
}

@Preview
@Composable
private fun SettingsXLoginFormPreview() {
    val scope = rememberCoroutineScope()
    val form = remember {
        XLoginFormState(SavedCredentials(login = "user@example.com", password = "", revision = 0), scope) { _, _, _ ->
            XSignInResult.Success
        }
    }
    SettingsXLoginForm(form = form, signInWebView = null, onClose = {})
}
