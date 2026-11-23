package com.client.xvideos.r.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.SavedTabHeader
import com.client.xvideos.common.ui.statusbar.NoStatusBarScrimEffect

internal object ScreenRedgifsLogin : Screen {
    override val key: ScreenKey = "RedgifsLogin"

    private fun readResolve(): Any = ScreenRedgifsLogin

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        RedgifsLoginContent(onClose = { navigator.pop() })
    }
}

@Composable
fun RedgifsLoginContent(onClose: () -> Unit, modifier: Modifier = Modifier) {
    NoStatusBarScrimEffect()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        SavedTabHeader(
            title = "Вход в RedGIFs",
            navigation = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
        )
        Text(
            text = "Введите почту и подтвердите вход кодом из письма.",
            color = Theme.Text.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        RSignInWebView(
            onSignedIn = {
                SnackBar.success("Вход в RedGIFs выполнен")
                onClose()
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}
