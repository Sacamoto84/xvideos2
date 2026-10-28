package com.client.xvideos.screenSettings.atom

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.screenSettings.components.SettingsRowTextSecondary

/** Шеврон «›» справа у строки, которая открывает другой экран. */
@Composable
internal fun SettingsChevron(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.exo_ic_chevron_right),
        contentDescription = null,
        tint = SettingsRowTextSecondary,
        modifier = modifier
    )
}

@Preview
@Composable
private fun SettingsChevronPreview() {
    SettingsChevron()
}
