package com.client.xvideos.screenSettings.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R

@Composable
fun SettingsValueRow(
    text: String,
    value: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = 0,
) {
    SettingsListItem(
        icon = icon,
        text = text,
        subtitle = value,
        modifier = modifier
    )
}

@Preview
@Composable
private fun SettingsValueRowPreview() {
    SettingsValueRow(
        icon = R.drawable.icon_red,
        text = "RAM кеш",
        value = "128 MB"
    )
}
