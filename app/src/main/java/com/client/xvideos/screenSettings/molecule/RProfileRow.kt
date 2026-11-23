package com.client.xvideos.screenSettings.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.auth.RAccountSession
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun RProfileRow(onLogin: () -> Unit) {
    val status by RAccountSession.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LaunchedEffect(status) {
        val account = status
        if (account is RAccountStatus.SignedIn) {
            delay((account.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
        }
        RAccountSession.checkExpiry()
    }

    val account = status
    when (account) {
        is RAccountStatus.SignedIn -> SettingsButtonRowWithDialog(
            icon = SettingsPage.Red.icon,
            text = "Профиль RedGIFs",
            value = "Выйти",
            subtitle = "@${account.username}",
            textDialogTitle = "Выйти из профиля RedGIFs?",
            textDialogBody = "Сессия RedGIFs будет удалена из приложения.",
            textDialogButton = "Выйти",
            onClick = {
                scope.launch {
                    if (runCatchingCancellable { RAccountSession.signOut() }.getOrDefault(false)) {
                        SnackBar.success("Вы вышли из профиля RedGIFs")
                    } else {
                        SnackBar.error("Не удалось удалить сессию RedGIFs")
                    }
                }
            },
        )
        RAccountStatus.Loading -> SettingsListItem(
            icon = SettingsPage.Red.icon,
            text = "Профиль RedGIFs",
            subtitle = "Проверяем сессию…",
        )
        else -> SettingsButtonRow(
            icon = SettingsPage.Red.icon,
            text = "Профиль RedGIFs",
            value = "Войти",
            subtitle = if (account == RAccountStatus.Expired) "Сессия истекла. Войдите снова." else "Вход не выполнен",
            onClick = onLogin,
        )
    }
}
