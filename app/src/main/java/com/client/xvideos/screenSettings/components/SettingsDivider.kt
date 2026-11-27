package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Разделитель строк. Прямо в [SettingsGroup] он становится зазором между
 * блоками; внутри вложенного блока остаётся линией.
 */
@Composable
fun SettingsDivider(startIndent: Dp = 56.dp) {
    val inGroup = LocalSettingsInGroup.current
    val containerModifier = if (inGroup) {
        Modifier
            .layoutId(SettingsGroupGap)
            .fillMaxWidth()
            .background(SettingsCardColor)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(SettingsCardColor)
    }
    val dividerModifier = Modifier.padding(start = startIndent, end = 16.dp)
    Box(modifier = containerModifier) {
        HorizontalDivider(
            modifier = dividerModifier,
            thickness = 0.5.dp,
            color = SettingsDividerColor
        )
    }
}

@Preview
@Composable
private fun SettingsDividerPreview() {
    SettingsDivider()
}
