package com.client.xvideos.common.settings.ui.section

import com.client.xvideos.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.settings.ui.Config_G_0_4
import com.client.xvideos.common.settings.ui.components.SettingsButtonRowWithDialog
import com.client.xvideos.common.settings.ui.components.SettingsDivider
import com.client.xvideos.common.settings.ui.components.SettingsGroup
import com.client.xvideos.common.settings.ui.components.SettingsValueRow
import com.client.xvideos.common.settings.ThumbnailsSize
import com.client.xvideos.common.settings.ui.components.ThumbnailSizeSelector
import com.client.xvideos.common.snackbar.SnackBar

@Composable
internal fun LSettingsSection(lLogin: String) {
    SettingsGroup {
        SettingsButtonRowWithDialog(
            icon = R.drawable.icon_luscious,
            text = "Профиль L",
            value = if (lLogin.isBlank()) "Нет" else "Выйти",
            textDialogTitle = "Выйти из профиля L",
            textDialogBody = if (lLogin.isBlank()) {
                "Вы не авторизованы в L."
            } else {
                "При следующем открытии L нужно будет снова ввести логин и пароль: $lLogin"
            },
            textDialogButton = "Выйти",
            onClick = {
                Settings.l_login.setValue("")
                Settings.l_pass.setValue("")
                SnackBar.success("Профиль L закрыт")
            }
        )
        SettingsDivider()

        val thumbnailSize = Settings.thumbalistSize.field.collectAsStateWithLifecycle().value
        val currentDisplayName = ThumbnailsSize.fromValue(thumbnailSize)?.displayName ?: "?"
        SettingsValueRow(
            icon = R.drawable.icon_luscious,
            text = "Размер миниатюры",
            value = currentDisplayName
        )
        ThumbnailSizeSelector(
            currentValue = currentDisplayName,
            onSelected = { selectedDisplayName ->
                ThumbnailsSize.fromDisplayName(selectedDisplayName)?.apply {
                    Settings.thumbalistSize.setValue(value)
                    SnackBar.success("Размер миниатюры: $displayName")
                }
            }
        )
        SettingsDivider()

        Config_G_0_4("L Gifs", Settings.l_gifsTab_G_0_4)
        Config_G_0_4("L Likes", Settings.l_likesTab_G_0_4)
        Config_G_0_4("L Collection", Settings.l_collectionTab_G_0_4)
    }
}
