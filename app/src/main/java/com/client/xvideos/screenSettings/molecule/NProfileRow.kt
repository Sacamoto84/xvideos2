package com.client.xvideos.screenSettings.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Строка профиля nhentai в настройках: отображает статус авторизации и управляет входом/выходом.
 */
@Composable
internal fun NProfileRow(
    username: String,
    hasAccount: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    if (hasAccount) {
        SettingsButtonRowWithDialog(
            icon = SettingsPage.N.icon,
            text = "Профиль nhentai",
            value = "Выйти",
            textDialogTitle = "Выйти из профиля nhentai?",
            textDialogBody = "Сессия $username будет удалена с устройства, лимиты API вернутся к базовым значениям.",
            textDialogButton = "Выйти",
            subtitle = "$username • повышенные лимиты API",
            onClick = onLogout
        )
    } else {
        SettingsButtonRow(
            icon = SettingsPage.N.icon,
            text = "Профиль nhentai",
            value = "Войти",
            subtitle = "Авторизация увеличивает лимиты запросов к API",
            onClick = onLogin
        )
    }
}

@Preview
@Composable
private fun NProfileRowPreview() {
    SettingsPreview {
        SettingsGroup {
            NProfileRow(
                username = "OtakuUser",
                hasAccount = true,
                onLogin = {},
                onLogout = {}
            )
        }
    }
}
