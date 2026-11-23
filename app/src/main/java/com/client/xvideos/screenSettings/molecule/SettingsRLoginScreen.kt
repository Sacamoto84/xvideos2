package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.r.auth.RSignInWebView
import com.client.xvideos.screenSettings.atom.SettingsTitleBar
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsScreenBackground

@Composable
internal fun SettingsRLoginScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SettingsScreenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        SettingsTitleBar(title = "Вход в RedGIFs", onBack = onClose)
        Text(
            text = "Введите почту и подтвердите вход кодом из письма.",
            color = SettingsRowTextPrimary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
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
