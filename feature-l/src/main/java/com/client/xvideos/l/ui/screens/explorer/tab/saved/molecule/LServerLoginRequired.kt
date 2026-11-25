package com.client.xvideos.l.ui.screens.explorer.tab.saved.molecule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.ui.screens.LLoginFormState
import com.client.xvideos.l.ui.screens.molecule.LLoginForm

@Composable
internal fun LServerLoginRequired(message: String) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Theme.background)
            .padding(top = getTopInsetDp())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = { navigator.push(LServerLoginScreen) }) {
            Text("Войти в L")
        }
    }
}

private object LServerLoginScreen : Screen {
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
        BackHandler(onBack = close)
        LLoginForm(
            login = form.login,
            password = form.password,
            onLoginChange = form::updateLogin,
            onPasswordChange = form::updatePassword,
            onSaved = close,
            onBack = close,
            onSkip = null,
        )
    }
}

private class LServerLoginScreenModel : ScreenModel {
    val form = LLoginFormState(Settings.l_profile.field.value)

    override fun onDispose() = form.clear()
}
