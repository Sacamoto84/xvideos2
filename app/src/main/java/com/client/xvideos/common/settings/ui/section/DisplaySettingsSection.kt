package com.client.xvideos.common.settings.ui.section

import com.client.xvideos.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.settings.ui.components.SettingsGroup
import com.client.xvideos.common.settings.ui.components.SettingsSwitchRow

@Composable
internal fun DisplaySettingsSection() {
    val useCutout = Settings.useCutoutPadding.field.collectAsStateWithLifecycle().value
    SettingsGroup {
        SettingsSwitchRow(
            icon = R.drawable.crop_free,
            text = "Учитывать верхний вырез",
            subtitle = if (useCutout) "Включено" else "Выключено",
            value = useCutout,
            onValueChange = { Settings.useCutoutPadding.setValue(it) }
        )
    }
}
