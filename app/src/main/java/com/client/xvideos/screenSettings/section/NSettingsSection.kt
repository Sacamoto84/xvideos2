package com.client.xvideos.screenSettings.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.R
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.n.net.NAuthState
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.ui.molecule.NCloudflareChallengeDialog
import com.client.xvideos.n.ui.molecule.NLoginDialog
import com.client.xvideos.screenSettings.Config_G_0_4
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.screenSettings.molecule.NProfileRow

@Composable
internal fun NSettingsSection(
    modifier: Modifier = Modifier
) {
    val isVerticalReader by Settings.n_fullscreen_vertical_pager.field.collectAsStateWithLifecycle()
    val isLoggedIn by NAuthState.isLoggedIn.collectAsStateWithLifecycle()
    val username by NAuthState.username.collectAsStateWithLifecycle()
    val challengeUrl by NCloudflareState.challengeUrl.collectAsStateWithLifecycle()
    var loginDialogVisible by remember { mutableStateOf(false) }

    val onToggleReader = remember(isVerticalReader) {
        {
            val next = !isVerticalReader
            Settings.n_fullscreen_vertical_pager.setValue(next)
            SnackBar.success(if (next) "Режим ридера: вертикальный скролл" else "Режим ридера: постраничный")
        }
    }

    val onVerifyCloudflare = remember {
        {
            NCloudflareState.triggerChallenge("https://nhentai.net")
        }
    }

    val onLogout = remember {
        {
            NAuthState.logout()
            SnackBar.success("Вы вышли из профиля nhentai")
        }
    }

    SettingsGroup(modifier = modifier) {
        NProfileRow(
            username = username,
            hasAccount = isLoggedIn,
            onLogin = { loginDialogVisible = true },
            onLogout = onLogout
        )
        SettingsDivider()

        SettingsValueRow(
            icon = R.drawable.icon_nhentai,
            text = "Ориентация ридера",
            value = if (isVerticalReader) "Вертикальная" else "Постраничная"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleReader)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Сменить режим чтения",
                color = Color(0xFFED2553),
                fontSize = 14.sp
            )
        }
        SettingsDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onVerifyCloudflare)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Обновить доступ Cloudflare (WebView)",
                color = Color.White,
                fontSize = 14.sp
            )
        }
        SettingsDivider()

        Config_G_0_4("N Манга", Settings.n_galleriesTab_G_0_4)
        Config_G_0_4("N Лайки", Settings.n_likesTab_G_0_4)
        Config_G_0_4("N Коллекции", Settings.n_collectionTab_G_0_4)
    }

    if (loginDialogVisible) {
        NLoginDialog(
            onDismiss = { loginDialogVisible = false },
            onSuccess = { name, sessionId ->
                NAuthState.onLoginSuccess(name, sessionId)
                loginDialogVisible = false
                SnackBar.success("Вход выполнен: $name. Лимиты API повышены!")
            }
        )
    }

    challengeUrl?.let { url ->
        NCloudflareChallengeDialog(
            url = url,
            onDismiss = { NCloudflareState.onChallengeDismissed() },
            onSuccess = { cookies ->
                NCloudflareState.onChallengeCompleted(cookies)
                SnackBar.success("Доступ Cloudflare успешно обновлён!")
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun NSettingsSectionPreview() = SettingsPreview {
    NSettingsSection()
}
