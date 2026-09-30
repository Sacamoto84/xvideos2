package com.client.xvideos.screenSettings.atom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.SettingsPage
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview

@Composable
internal fun SettingsNavigationItem(
    page: SettingsPage,
    onOpenPage: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = remember(page, onOpenPage) { { onOpenPage(page) } }
    SettingsListItem(
        icon = page.icon,
        text = page.title,
        subtitle = page.subtitle,
        onClick = onClick,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsNavigationItemPreview() = SettingsPreview {
    SettingsNavigationItem(
        page = SettingsPage.Privacy,
        onOpenPage = {}
    )
}
