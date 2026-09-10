package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsSwitchRow

@Composable
internal fun XSettingsSection() {
    val xvideosRow2 = Settings.xvideos_row2.field.collectAsStateWithLifecycle().value
    SettingsGroup {
        SettingsSwitchRow(
            icon = R.drawable.icon_xvideos_white,
            text = "2 столбика",
            subtitle = if (xvideosRow2) "Включено" else "Выключено",
            value = xvideosRow2,
            onValueChange = { Settings.xvideos_row2.setValue(it) }
        )
        SettingsDivider()

        val xvideosShemale = Settings.xvideos_shemale.field.collectAsStateWithLifecycle().value
        SettingsSwitchRow(
            icon = R.drawable.icon_xvideos_white,
            text = "Shemale",
            subtitle = if (xvideosShemale) "Включено" else "Выключено",
            value = xvideosShemale,
            onValueChange = { Settings.xvideos_shemale.setValue(it) }
        )
    }
}
