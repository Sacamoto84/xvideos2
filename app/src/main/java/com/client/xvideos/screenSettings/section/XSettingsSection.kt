package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSwitchRow

@Composable
internal fun XSettingsSection() {
    val xvideosRow2 by Settings.xvideos_row2.field.collectAsStateWithLifecycle()
    val xvideosShemale by Settings.xvideos_shemale.field.collectAsStateWithLifecycle()

    SettingsGroup {
        SettingsSwitchRow(
            icon = R.drawable.icon_xvideos_white,
            text = "2 столбика",
            subtitle = if (xvideosRow2) "Включено" else "Выключено",
            value = xvideosRow2,
            onValueChange = { Settings.xvideos_row2.setValue(it) }
        )
        SettingsDivider()

        SettingsSwitchRow(
            icon = R.drawable.icon_xvideos_white,
            text = "Shemale",
            subtitle = if (xvideosShemale) "Включено" else "Выключено",
            value = xvideosShemale,
            onValueChange = { Settings.xvideos_shemale.setValue(it) }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun XSettingsSectionPreview() = SettingsPreview {
    XSettingsSection()
}
