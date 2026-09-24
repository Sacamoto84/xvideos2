package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.Config_G_0_4
import com.client.xvideos.screenSettings.components.SettingsButtonRowWithDialog
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.common.settings.ThumbnailsSize
import com.client.xvideos.screenSettings.components.ThumbnailSizeSelector
import com.client.xvideos.common.snackbar.SnackBar

private const val TEXT_NO = "Нет"
private const val TEXT_LOGOUT = "Выйти"
private const val TEXT_PROFILE_L = "Профиль L"
private const val TEXT_LOGOUT_DIALOG_TITLE = "Выйти из профиля L"
private const val TEXT_THUMBNAIL_SIZE = "Размер миниатюры"
private const val TEXT_NOT_AUTHORIZED = "Вы не авторизованы в L."
private const val TEXT_UNKNOWN_SIZE = "?"
private const val TAB_L_GIFS = "L Gifs"
private const val TAB_L_LIKES = "L Likes"
private const val TAB_L_COLLECTION = "L Collection"

@Composable
internal fun LSettingsSection(lLogin: String) {
    val thumbnailSize by Settings.thumbalistSize.field.collectAsStateWithLifecycle()
    val currentDisplayName = remember(thumbnailSize) {
        ThumbnailsSize.fromValue(thumbnailSize)?.displayName ?: TEXT_UNKNOWN_SIZE
    }

    val isLoginBlank = remember(lLogin) { lLogin.isBlank() }
    val loginValueText = remember(isLoginBlank) { if (isLoginBlank) TEXT_NO else TEXT_LOGOUT }
    val logoutDialogBody = remember(isLoginBlank, lLogin) {
        if (isLoginBlank) {
            TEXT_NOT_AUTHORIZED
        } else {
            "При следующем открытии L нужно будет снова ввести логин и пароль: $lLogin"
        }
    }

    val onLogoutL = remember {
        {
            Settings.l_login.setValue("")
            Settings.l_pass.setValue("")
            SnackBar.success("Профиль L закрыт")
        }
    }
    val onSelectThumbnailSize: (String) -> Unit = remember {
        { selectedDisplayName ->
            ThumbnailsSize.fromDisplayName(selectedDisplayName)?.apply {
                Settings.thumbalistSize.setValue(value)
                SnackBar.success("Размер миниатюры: $displayName")
            }
        }
    }

    SettingsGroup {
        SettingsButtonRowWithDialog(
            icon = R.drawable.icon_luscious,
            text = TEXT_PROFILE_L,
            value = loginValueText,
            textDialogTitle = TEXT_LOGOUT_DIALOG_TITLE,
            textDialogBody = logoutDialogBody,
            textDialogButton = TEXT_LOGOUT,
            onClick = onLogoutL
        )
        SettingsDivider()
        SettingsValueRow(
            icon = R.drawable.icon_luscious,
            text = TEXT_THUMBNAIL_SIZE,
            value = currentDisplayName
        )
        ThumbnailSizeSelector(
            currentValue = currentDisplayName,
            onSelected = onSelectThumbnailSize
        )
        SettingsDivider()

        Config_G_0_4(TAB_L_GIFS, Settings.l_gifsTab_G_0_4)
        Config_G_0_4(TAB_L_LIKES, Settings.l_likesTab_G_0_4)
        Config_G_0_4(TAB_L_COLLECTION, Settings.l_collectionTab_G_0_4)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun LSettingsSectionPreview() = SettingsPreview {
    LSettingsSection(lLogin = "preview_user")
}
