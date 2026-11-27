package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview

/** Разделитель полосой фона экрана; прямо в [SettingsGroup] — зазор между блоками. */
@Composable
fun SettingsDivider2() {
    Spacer(
        Modifier
            .layoutId(SettingsGroupGap)
            .fillMaxWidth()
            .height(SettingsGroupGapHeight)
            .background(SettingsScreenBackground)
    )
}

@Preview
@Composable
private fun SettingsDivider2Preview() {
    SettingsDivider2()
}
