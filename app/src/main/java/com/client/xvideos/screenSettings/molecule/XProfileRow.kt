package com.client.xvideos.screenSettings.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Строка профиля X в настройках — как [LProfileRow]: без аккаунта «Войти»
 * открывает форму входа, с аккаунтом «Выйти» сначала спрашивает подтверждение.
 *
 * Третье состояние есть только у X: профиль сохранён, но сайт не принял его
 * при автоматическом входе (сменили пароль на сайте). Тогда строка снова
 * предлагает «Войти» и показывает ответ сайта — форма откроется с логином.
 *
 * @param login сохранённый логин; показывается, когда [hasAccount].
 * @param hasAccount сохранены и логин, и пароль.
 * @param restoreFailure ответ сайта на сохранённый профиль; `null` — отказа не было.
 */
@Composable
internal fun XProfileRow(
    login: String,
    hasAccount: Boolean,
    restoreFailure: String?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    when {
        hasAccount && restoreFailure == null -> SettingsButtonRowWithDialog(
            icon = SettingsPage.X.icon,
            text = "Профиль X",
            value = "Выйти",
            textDialogTitle = "Выйти из профиля X?",
            textDialogBody = "Сессия на сайте закроется, логин и пароль $login будут удалены с устройства. " +
                "Войти снова можно в настройках X.",
            textDialogButton = "Выйти",
            subtitle = login,
            onClick = onLogout
        )
        hasAccount -> SettingsButtonRow(
            icon = SettingsPage.X.icon,
            text = "Профиль X",
            value = "Войти",
            subtitle = "$login — $restoreFailure",
            onClick = onLogin
        )
        else -> SettingsButtonRow(
            icon = SettingsPage.X.icon,
            text = "Профиль X",
            value = "Войти",
            subtitle = "Вход не выполнен",
            onClick = onLogin
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun XProfileRowPreview() = SettingsPreview {
    SettingsGroup {
        XProfileRow(login = "", hasAccount = false, restoreFailure = null, onLogin = {}, onLogout = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun XProfileRowWithAccountPreview() = SettingsPreview {
    SettingsGroup {
        XProfileRow(login = "user@example.com", hasAccount = true, restoreFailure = null, onLogin = {}, onLogout = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun XProfileRowRejectedPreview() = SettingsPreview {
    SettingsGroup {
        XProfileRow(
            login = "user@example.com",
            hasAccount = true,
            restoreFailure = "Не удается войти. Пожалуйста, проверьте свой логин/пароль.",
            onLogin = {},
            onLogout = {}
        )
    }
}
