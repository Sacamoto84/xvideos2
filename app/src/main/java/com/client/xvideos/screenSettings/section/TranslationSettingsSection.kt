package com.client.xvideos.screenSettings.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.translation.TranslationEngine
import com.client.xvideos.screenSettings.molecule.TranslationSettingsContent

@Composable
internal fun TranslationSettingsSection(modifier: Modifier = Modifier) {
    val engine by Settings.translation_engine.field.collectAsStateWithLifecycle()
    TranslationSettingsContent(
        selectedEngine = TranslationEngine.fromStoredValue(engine),
        onSelect = { Settings.translation_engine.setValue(it.name) },
        modifier = modifier
    )
}

@Preview
@Composable
private fun TranslationSettingsSectionPreview() {
    TranslationSettingsSection()
}
