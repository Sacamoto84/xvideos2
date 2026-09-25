package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSwitchRow

private const val TEXT_TWO_COLUMNS = "2 столбика"
private const val TEXT_SHEMALE = "Shemale"
private const val TEXT_ENABLED = "Включено"
private const val TEXT_DISABLED = "Выключено"
private const val ICON_XVIDEOS = R.drawable.icon_xvideos_white

@Composable
internal fun XSettingsSection(
    modifier: Modifier = Modifier,
) {
    val xvideosRow2 by Settings.xvideos_row2.field.collectAsStateWithLifecycle()
    val xvideosShemale by Settings.xvideos_shemale.field.collectAsStateWithLifecycle()

    val onRow2Change: (Boolean) -> Unit = remember { { enabled -> Settings.xvideos_row2.setValue(enabled) } }
    val onShemaleChange: (Boolean) -> Unit = remember { { enabled -> Settings.xvideos_shemale.setValue(enabled) } }

    val row2Subtitle = remember(xvideosRow2) { if (xvideosRow2) TEXT_ENABLED else TEXT_DISABLED }
    val shemaleSubtitle = remember(xvideosShemale) { if (xvideosShemale) TEXT_ENABLED else TEXT_DISABLED }

    SettingsGroup(modifier = modifier) {
        SettingsSwitchRow(
            icon = ICON_XVIDEOS,
            text = TEXT_TWO_COLUMNS,
            subtitle = row2Subtitle,
            value = xvideosRow2,
            onValueChange = onRow2Change
        )
        SettingsDivider()

        SettingsSwitchRow(
            icon = ICON_XVIDEOS,
            text = TEXT_SHEMALE,
            subtitle = shemaleSubtitle,
            value = xvideosShemale,
            onValueChange = onShemaleChange
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun XSettingsSectionPreview() = SettingsPreview {
    XSettingsSection(modifier = Modifier)
}
