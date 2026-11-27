package com.client.xvideos.screenSettings.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.rotation.SystemRotationSettings
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.screenSettings.molecule.RotationLockRow
import com.client.xvideos.screenSettings.molecule.ScrollButtonPreviewCard
import com.client.xvideos.screenSettings.molecule.ScrollEffectItem
import dev.chrisbanes.haze.rememberHazeState

/**
 * Экран настроек «Отображение» (Appearance).
 */
@Composable
internal fun AppearanceSettingsSection(
    modifier: Modifier = Modifier,
) {
    val effectName by Settings.scroll_buttons_effect.field.collectAsStateWithLifecycle()
    val currentEffect = remember(effectName) { ScrollButtonEffect.fromNameOrDefault(effectName) }
    val previewHazeState = rememberHazeState()

    val onSelectEffect: (ScrollButtonEffect) -> Unit = remember {
        { effect -> Settings.scroll_buttons_effect.setValue(effect.name) }
    }

    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val canWriteSettings = remember(lifecycleState, isPreview) {
        !isPreview && SystemRotationSettings(context).canWrite()
    }
    val onOpenRotationPermission: () -> Unit = remember(context) {
        { SystemRotationSettings.openPermissionScreen(context) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle("Предпросмотр")
        ScrollButtonPreviewCard(
            hazeState = previewHazeState,
            currentEffect = currentEffect
        )

        Spacer(Modifier.height(8.dp))

        SettingsSectionTitle("Кнопки быстрой прокрутки")
        SettingsGroup {
            ScrollButtonEffect.entries.forEachIndexed { index, effect ->
                key(effect.name) {
                    if (index > 0) {
                        SettingsDivider()
                    }
                    ScrollEffectItem(
                        effect = effect,
                        isSelected = (currentEffect == effect),
                        onSelect = onSelectEffect
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        SettingsSectionTitle("Поворот экрана")
        SettingsGroup {
            RotationLockRow(
                canWriteSettings = canWriteSettings,
                onOpenPermission = onOpenRotationPermission
            )
        }
    }
}

@Preview
@Composable
private fun AppearanceSettingsSectionPreview() {
    AppearanceSettingsSection()
}
