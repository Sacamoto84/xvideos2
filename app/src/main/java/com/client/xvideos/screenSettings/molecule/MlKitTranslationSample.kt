package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R
import com.client.xvideos.common.translation.TextTranslator
import com.client.xvideos.common.translation.TranslationEngine
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsCardColor
import com.client.xvideos.screenSettings.components.SettingsDestructiveColor
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsOnAccentColor
import com.client.xvideos.screenSettings.components.SettingsRowTextPrimary
import com.client.xvideos.screenSettings.components.SettingsRowTextSecondary
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
internal fun MlKitTranslationSample(modifier: Modifier = Modifier) {
    val source = stringResource(R.string.translation_sample_source)
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    var translating by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
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
}

@Preview
@Composable
private fun MlKitTranslationSamplePreview() {
    MlKitTranslationSample()
}
