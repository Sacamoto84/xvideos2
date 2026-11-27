package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.translation.TranslationEngine
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.screenSettings.components.SettingsUncheckedColor

@Composable
internal fun TranslationSettingsContent(
    selectedEngine: TranslationEngine,
    onSelect: (TranslationEngine) -> Unit,
    modifier: Modifier = Modifier
) {
    val radioColors = RadioButtonDefaults.colors(
        selectedColor = SettingsAccentColor,
        unselectedColor = SettingsUncheckedColor
    )
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle(stringResource(R.string.translation_engine))
        SettingsGroup(modifier = Modifier.selectableGroup()) {
            TranslationEngine.entries.forEachIndexed { index, engine ->
                if (index > 0) SettingsDivider()
                val label = when (engine) {
                    TranslationEngine.NONE -> R.string.translation_engine_none
                    TranslationEngine.ML_KIT -> R.string.translation_engine_mlkit
                }
                val isSelected = engine == selectedEngine
                SettingsListItem(
                    text = stringResource(label),
                    subtitle = if (engine == TranslationEngine.ML_KIT) {
                        stringResource(R.string.translation_engine_mlkit_description)
                    } else null,
                    trailing = { RadioButton(selected = isSelected, onClick = null, colors = radioColors) },
                    onClick = { onSelect(engine) },
                    modifier = Modifier.semantics {
                        selected = isSelected
                        role = Role.RadioButton
                    }
                )
            }
        }
        if (selectedEngine == TranslationEngine.ML_KIT) MlKitTranslationSample()
    }
}

@Preview
@Composable
private fun TranslationSettingsContentPreview() {
    TranslationSettingsContent(selectedEngine = TranslationEngine.ML_KIT, onSelect = {})
}
