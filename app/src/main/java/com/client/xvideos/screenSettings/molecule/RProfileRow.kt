package com.client.xvideos.screenSettings.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.auth.RAccountSession
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import kotlinx.coroutines.launch

@Composable
internal fun RProfileRow(onLogin: () -> Unit) {
    val status by RAccountSession.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    if (!LocalInspectionMode.current) {
        LaunchedEffect(Unit) { RAccountSession.restore() }
    }

    when (val account = status) {
        is RAccountStatus.SignedIn -> SettingsButtonRowWithDialog(
            icon = SettingsPage.Red.icon,
            text = "Профиль R",
            value = "Выйти",
            subtitle = "@${account.username}",
            textDialogTitle = "Выйти из профиля R?",
            textDialogBody = "Сессия R будет удалена из приложения.",
            textDialogButton = "Выйти",
            onClick = {
                scope.launch {
                    if (runCatchingCancellable { RAccountSession.signOut() }.getOrDefault(false)) {
                        SnackBar.success("Вы вышли из профиля R")
                    } else {
                        SnackBar.error("Не удалось удалить сессию R")
                    }
                }
            },
        )
        RAccountStatus.Loading -> SettingsListItem(
            icon = SettingsPage.Red.icon,
            text = "Профиль R",
            subtitle = "Проверяем сессию…",
        )
        else -> SettingsButtonRow(
            icon = SettingsPage.Red.icon,
            text = "Профиль R",
            value = "Войти",
            subtitle = if (account == RAccountStatus.Expired) "Сессия истекла. Войдите снова." else "Вход не выполнен",
            onClick = onLogin,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun RProfileRowPreview() {
    SettingsGroup {
        RProfileRow(onLogin = {})
    }
}
