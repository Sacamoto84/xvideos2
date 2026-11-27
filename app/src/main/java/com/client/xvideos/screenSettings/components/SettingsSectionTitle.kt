package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

@Composable
fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val style = remember(Theme.L.Type.caption) {
        Theme.L.Type.caption.copy(
            color = SettingsAccentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp
        )
    }
    val titleModifier = modifier.padding(
        start = 24.dp,
        top = 20.dp,
        bottom = 8.dp,
        end = 24.dp
    )
    Text(
        text = text,
        modifier = titleModifier,
        color = SettingsAccentColor,
        style = style
    )
}

@Preview
@Composable
private fun SettingsSectionTitlePreview() {
    SettingsSectionTitle("Защита")
}
