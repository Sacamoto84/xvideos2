package com.client.xvideos.screenSettings.section

import com.client.xvideos.R

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.ui.choice.ChoiceRow
import com.client.xvideos.common.ui.choice.ChoiceRowStyle
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSwitchRow
import com.client.xvideos.screenSettings.components.SettingsValueRow
import com.client.xvideos.x.screens.videoplayer.model.XSeekPreviewMode
import kotlinx.collections.immutable.toPersistentList

private const val TEXT_ENABLED = "Включено"
private const val TEXT_DISABLED = "Выключено"
private const val ICON_XVIDEOS = R.drawable.icon_xvideos_white
private val SEEK_PREVIEW_OPTIONS = XSeekPreviewMode.entries.map { it.label }.toPersistentList()

@Composable
internal fun XSettingsSection(
    modifier: Modifier = Modifier,
) {
    val xvideosRow2 by Settings.xvideos_row2.field.collectAsStateWithLifecycle()
    val xvideosShemale by Settings.xvideos_shemale.field.collectAsStateWithLifecycle()
    val seekPreviewValue by Settings.x_seek_preview_mode.field.collectAsStateWithLifecycle()

    val onRow2Change: (Boolean) -> Unit = remember { { enabled -> Settings.xvideos_row2.setValue(enabled) } }
    val onShemaleChange: (Boolean) -> Unit = remember { { enabled -> Settings.xvideos_shemale.setValue(enabled) } }
    val onSeekPreviewSelect: (Int) -> Unit = remember {
        { index -> Settings.x_seek_preview_mode.setValue(XSeekPreviewMode.entries[index].settingValue) }
    }

    val row2Subtitle = if (xvideosRow2) TEXT_ENABLED else TEXT_DISABLED
    val shemaleSubtitle = if (xvideosShemale) TEXT_ENABLED else TEXT_DISABLED
    val seekPreviewMode = XSeekPreviewMode.fromSetting(seekPreviewValue)

    SettingsGroup(modifier = modifier) {
        SettingsSwitchRow(
            icon = ICON_XVIDEOS,
            text = "2 столбика",
            subtitle = row2Subtitle,
            value = xvideosRow2,
            onValueChange = onRow2Change
        )
        SettingsDivider()

        SettingsSwitchRow(
            icon = ICON_XVIDEOS,
            text = "Shemale",
            subtitle = shemaleSubtitle,
            value = xvideosShemale,
            onValueChange = onShemaleChange
        )
        SettingsDivider()

        // Вид превью кадра при перемотке в полноэкранном плеере X. Временная
        // кнопка на экране плеера пишет ту же настройку.
        SettingsValueRow(
            icon = ICON_XVIDEOS,
            text = "Превью при перемотке",
            value = seekPreviewMode.label
        )
        ChoiceRow(
            options = SEEK_PREVIEW_OPTIONS,
            selectedIndex = seekPreviewMode.ordinal,
            onSelectIndex = onSeekPreviewSelect,
            style = ChoiceRowStyle.MATERIAL,
            accent = SettingsAccentColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 72.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun XSettingsSectionPreview() = SettingsPreview {
    XSettingsSection(modifier = Modifier)
}
