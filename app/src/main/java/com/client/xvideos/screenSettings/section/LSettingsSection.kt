package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.Config_G_0_4
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.common.settings.ThumbnailsSize
import com.client.xvideos.screenSettings.components.ThumbnailSizeSelector
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.screenSettings.molecule.LProfileRow

@Composable
internal fun LSettingsSection(
    lLogin: String,
    hasLAccount: Boolean,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnailSize by Settings.thumbalistSize.field.collectAsStateWithLifecycle()
    val currentDisplayName = remember(thumbnailSize) {
        ThumbnailsSize.fromValue(thumbnailSize)?.displayName ?: "?"
    }

    // Стираем только сохранённый профиль: сессию закрывает репозиторий L, когда
    // на следующем запросе увидит, что профиля больше нет.
    val onLogoutL = remember {
        {
            Settings.l_profile.clear()
            SnackBar.success("Вы вышли из профиля L")
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

    SettingsGroup(modifier = modifier) {
        LProfileRow(
            login = lLogin,
            hasAccount = hasLAccount,
            onLogin = onLogin,
            onLogout = onLogoutL
        )
        SettingsDivider()
        SettingsValueRow(
            icon = R.drawable.icon_luscious,
            text = "Размер миниатюры",
            value = currentDisplayName
        )
        ThumbnailSizeSelector(
            currentValue = currentDisplayName,
            onSelected = onSelectThumbnailSize
        )
        SettingsDivider()

        Config_G_0_4("L Gifs", Settings.l_gifsTab_G_0_4)
        Config_G_0_4("L Likes", Settings.l_likesTab_G_0_4)
        Config_G_0_4("L Collection", Settings.l_collectionTab_G_0_4)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun LSettingsSectionPreview() = SettingsPreview {
    LSettingsSection(lLogin = "preview_user", hasLAccount = true, onLogin = {}, modifier = Modifier)
}
