package com.client.xvideos.l.ui.screens.explorer.tab.saved

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.ui.screens.LLoginFormState
import com.client.xvideos.l.ui.screens.molecule.LLoginForm

internal object LServerLoginScreen : Screen {
    override val key = "LServerLoginScreen"

    private fun readResolve(): Any = LServerLoginScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val form = rememberScreenModel { LServerLoginScreenModel() }.form
        val close: () -> Unit = remember(navigator, form) {
            {
                form.clear()
                navigator.pop()
            }
        }
        ScreenLServerLoginContent(
            login = form.login,
            password = form.password,
            onLoginChange = form::updateLogin,
            onPasswordChange = form::updatePassword,
            onClose = close,
        )
    }
}

@Composable
internal fun ScreenLServerLoginContent(
    login: String,
    password: String,
    onLoginChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    LLoginForm(
        login = login,
        password = password,
        onLoginChange = onLoginChange,
        onPasswordChange = onPasswordChange,
        onSaved = onClose,
        onBack = onClose,
        onSkip = null,
    )
}

private class LServerLoginScreenModel : ScreenModel {
    val form = LLoginFormState(Settings.l_profile.field.value)

    override fun onDispose() = form.clear()
}

@Preview
@Composable
private fun LServerLoginScreenPreview() {
    ScreenLServerLoginContent(
        login = "",
        password = "",
        onLoginChange = {},
        onPasswordChange = {},
        onClose = {},
    )
}
