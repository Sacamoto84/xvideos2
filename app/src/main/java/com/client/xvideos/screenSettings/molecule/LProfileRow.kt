package com.client.xvideos.screenSettings.molecule

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.DialogButton
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Строка профиля L в настройках: показывает, сохранён ли аккаунт, и предлагает
 * действие, которое этому состоянию соответствует.
 *
 * Без аккаунта «Войти» сразу зовёт [onLogin]. С аккаунтом «Выйти» сначала
 * спрашивает подтверждение. Раньше кнопка в обоих случаях открывала диалог
 * выхода, и без аккаунта он предлагал выйти из профиля, которого нет.
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
    var logoutDialogVisible by remember { mutableStateOf(false) }
    val onDismissLogout = remember { { logoutDialogVisible = false } }
    val onAskLogout = remember { { logoutDialogVisible = true } }

    DialogButton(
        visible = logoutDialogVisible,
        title = "Выйти из профиля L?",
        body = "Логин и пароль $login будут удалены с устройства. L продолжит работать без авторизации.",
        buttonText = "Выйти",
        onDismiss = onDismissLogout,
        onBlockConfirmed = onLogout
    )

    val buttonText = if (hasAccount) "Выйти" else "Войти"
    val onButtonClick = if (hasAccount) onAskLogout else onLogin
    val trailing: @Composable () -> Unit = remember(buttonText, onButtonClick) {
        {
            TextButton(onClick = onButtonClick) {
                Text(buttonText, color = SettingsAccentColor, fontWeight = FontWeight.Medium)
            }
        }
    }

    SettingsListItem(
        icon = SettingsPage.L.icon,
        text = "Профиль L",
        subtitle = if (hasAccount) login else "Вход не выполнен",
        trailing = trailing
    )
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
