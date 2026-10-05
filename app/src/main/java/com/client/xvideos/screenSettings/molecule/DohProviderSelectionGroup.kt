package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.net.doh.DohProvider
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSectionTitle

@Composable
internal fun DohProviderSelectionGroup(
    currentProvider: DohProvider,
    customUrl: String,
    onSelectProvider: (DohProvider) -> Unit,
    onOpenCustomUrlDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val customSubtitle = remember(customUrl) {
        if (customUrl.isNotBlank()) customUrl else "Нажмите для ввода URL"
    }
    val radioColors = RadioButtonDefaults.colors(
        selectedColor = SettingsAccentColor,
        unselectedColor = Color(0xFF938F99)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle("Провайдер DNS")
        SettingsGroup {
            DohProvider.entries.forEachIndexed { index, provider ->
                key(provider.name) {
                    if (index > 0) SettingsDivider()

                    val subtitle = remember(provider, customUrl) {
                        if (provider == DohProvider.CUSTOM) {
                            if (customUrl.isNotBlank()) customUrl else provider.description
                        } else {
                            provider.description
                        }
                    }

                    DohProviderItem(
                        provider = provider,
                        isSelected = currentProvider == provider,
                        subtitle = subtitle,
                        radioColors = radioColors,
                        onSelect = onSelectProvider
                    )
                }
            }

            if (currentProvider == DohProvider.CUSTOM) {
                SettingsDivider()
                SettingsListItem(
                    icon = R.drawable.ic_dns_24,
                    text = "Адрес DoH сервера",
                    subtitle = customSubtitle,
                    onClick = onOpenCustomUrlDialog
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun DohProviderSelectionGroupPreview() = SettingsPreview {
    DohProviderSelectionGroup(
        currentProvider = DohProvider.CLOUDFLARE,
        customUrl = "",
        onSelectProvider = {},
        onOpenCustomUrlDialog = {}
    )
}
