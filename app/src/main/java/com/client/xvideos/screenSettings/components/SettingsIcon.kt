package com.client.xvideos.screenSettings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R

@Composable
fun SettingsIcon(@DrawableRes icon: Int) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = SettingsRowTextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview
@Composable
private fun SettingsIconPreview() {
    SettingsIcon(R.drawable.icon_red)
}
