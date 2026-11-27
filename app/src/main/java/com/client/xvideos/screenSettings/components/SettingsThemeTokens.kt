package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import kotlin.math.roundToInt

internal val SettingsScreenBackground = Theme.background
internal val SettingsTopBarColor = SettingsScreenBackground
internal val SettingsCardColor = Theme.tabLevel2
internal val SettingsAccentColor = Color(0xFFC4C0FD)
internal val SettingsOnAccentColor = Color(0xFF2E2961)
internal val SettingsRowTextPrimary = Theme.Text.primary
internal val SettingsRowTextSecondary = Theme.Text.secondary
internal val SettingsDividerColor = Color(0x2E7A7A7A)
internal val SettingsUncheckedColor = Theme.L.grey2
internal val SettingsDestructiveColor = Color(0xFFFF7A7A)
internal val WhatsAppGreen = SettingsAccentColor
internal val SettingsCardCorner = 24.dp
internal val settingsCardShape = RoundedCornerShape(SettingsCardCorner)
internal val SettingsGroupGapHeight = 2.dp
internal val SettingsGroupInnerCorner = 4.dp

internal val SETTINGS_ITEM_CORE_MODIFIER = Modifier
    .background(SettingsCardColor)
    .heightIn(min = 60.dp)
    .padding(horizontal = 16.dp, vertical = 13.dp)

internal val SETTINGS_ITEM_IN_GROUP_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .then(SETTINGS_ITEM_CORE_MODIFIER)

internal val SETTINGS_ITEM_STANDALONE_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp)
    .clip(settingsCardShape)
    .then(SETTINGS_ITEM_CORE_MODIFIER)

internal val LocalSettingsInGroup = staticCompositionLocalOf { false }

internal object SettingsGroupGap

fun snapSliderValue(value: Float, min: Int, max: Int, step: Int): Int {
    val safeStep = step.coerceAtLeast(1)
    val shifted = (value.roundToInt() - min).coerceAtLeast(0)
    return (min + ((shifted + safeStep / 2) / safeStep) * safeStep).coerceIn(min, max)
}
