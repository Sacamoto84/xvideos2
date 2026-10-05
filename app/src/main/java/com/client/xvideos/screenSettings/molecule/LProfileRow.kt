package com.client.xvideos.screenSettings.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Строка профиля L в настройках: показывает, сохранён ли аккаунт, и предлагает
 * действие, которое этому состоянию соответствует.
 *
 * Без аккаунта «Войти» сразу зовёт [onLogin]. С аккаунтом «Выйти» сначала
 * спрашивает подтверждение. Раньше кнопка в обоих случаях открывала диалог
 * выхода, и без аккаунта он предлагал выйти из профиля, которого нет.
 *
 * Диалог не обещает, что L продолжит работать без авторизации: после выхода
 * раздел L снова показывает форму входа, если её не пропускали.
 *
 * @param login сохранённый логин; показывается, когда [hasAccount].
 * @param hasAccount сохранены и логин, и пароль.
 */
@Composable
internal fun LProfileRow(
    login: String,
    hasAccount: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    if (hasAccount) {
        SettingsButtonRowWithDialog(
            icon = SettingsPage.L.icon,
            text = "Профиль L",
            value = "Выйти",
            textDialogTitle = "Выйти из профиля L?",
            textDialogBody = "Логин и пароль $login будут удалены с устройства. Войти снова можно в настройках L.",
            textDialogButton = "Выйти",
            subtitle = login,
            onClick = onLogout
        )
    } else {
        SettingsButtonRow(
            icon = SettingsPage.L.icon,
            text = "Профиль L",
            value = "Войти",
            subtitle = "Вход не выполнен",
            onClick = onLogin
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun LProfileRowPreview() = SettingsPreview {
    SettingsGroup {
        LProfileRow(login = "", hasAccount = false, onLogin = {}, onLogout = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun LProfileRowWithAccountPreview() = SettingsPreview {
    SettingsGroup {
        LProfileRow(login = "preview_user", hasAccount = true, onLogin = {}, onLogout = {})
    }
}
