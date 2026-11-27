package com.client.xvideos.screenSettings.molecule

import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.settings.ScrollButtonEffect
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsUncheckedColor

@Composable
internal fun ScrollEffectItem(
    effect: ScrollButtonEffect,
    isSelected: Boolean,
    onSelect: (ScrollButtonEffect) -> Unit,
    modifier: Modifier = Modifier
) {
    val onClick = remember(effect, onSelect) { { onSelect(effect) } }
    val radioColors = RadioButtonDefaults.colors(
        selectedColor = SettingsAccentColor,
        unselectedColor = SettingsUncheckedColor
    )
    val trailingContent: @Composable () -> Unit = remember(isSelected, radioColors) {
        {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = radioColors
            )
        }
    }
    SettingsListItem(
        icon = R.drawable.ic_blur_24,
        text = effect.title,
        subtitle = effect.subtitle,
        trailing = trailingContent,
        onClick = onClick,
        modifier = modifier
    )
}

@Preview
@Composable
private fun ScrollEffectItemPreview() {
    ScrollEffectItem(
        effect = ScrollButtonEffect.entries.first(),
        isSelected = true,
        onSelect = {},
    )
}
