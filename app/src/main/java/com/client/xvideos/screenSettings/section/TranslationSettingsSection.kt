package com.client.xvideos.screenSettings.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.R
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.translation.TextTranslator
import com.client.xvideos.common.translation.TranslationEngine
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsCardColor
import com.client.xvideos.screenSettings.components.SettingsDestructiveColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsOnAccentColor
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsRowTextSecondary
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.screenSettings.components.SettingsUncheckedColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
internal fun TranslationSettingsSection(modifier: Modifier = Modifier) {
    val engine by Settings.translation_engine.field.collectAsStateWithLifecycle()
    TranslationSettingsContent(
        selectedEngine = TranslationEngine.fromStoredValue(engine),
        onSelect = { Settings.translation_engine.setValue(it.name) },
        modifier = modifier
    )
}

@Composable
private fun TranslationSettingsContent(
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

@Composable
private fun MlKitTranslationSample() {
    val source = stringResource(R.string.translation_sample_source)
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    var translating by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    SettingsSectionTitle(stringResource(R.string.translation_sample))
    SettingsGroup {
        Column(
            modifier = Modifier.fillMaxWidth().background(SettingsCardColor).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(source, color = SettingsRowTextPrimary, style = MaterialTheme.typography.bodyLarge)
            Button(
                enabled = !translating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsAccentColor,
                    contentColor = SettingsOnAccentColor
                ),
                onClick = {
                    translating = true
                    failed = false
                    result = null
                    scope.launch {
                        try {
                            result = TextTranslator.translate(source, "en", "ru", TranslationEngine.ML_KIT)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (failure: Exception) {
                            Timber.w(failure, "ML Kit sample translation failed")
                            failed = true
                        } finally {
                            translating = false
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.translation_run_sample))
            }
            if (translating) {
                Text(stringResource(R.string.translation_in_progress), color = SettingsRowTextSecondary)
            }
            if (failed) {
                Text(stringResource(R.string.translation_failed), color = SettingsDestructiveColor)
            }
            result?.let { translated ->
                Text(translated, color = SettingsRowTextPrimary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626, widthDp = 360)
@Composable
private fun TranslationSettingsSectionPreview() = SettingsPreview {
    TranslationSettingsContent(selectedEngine = TranslationEngine.ML_KIT, onSelect = {})
}
