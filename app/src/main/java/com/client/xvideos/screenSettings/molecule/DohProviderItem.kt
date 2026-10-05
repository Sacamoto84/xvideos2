package com.client.xvideos.screenSettings.molecule

import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.net.doh.DohProvider
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview

@Composable
internal fun DohProviderItem(
    provider: DohProvider,
    isSelected: Boolean,
    subtitle: String,
    radioColors: androidx.compose.material3.RadioButtonColors,
    onSelect: (DohProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = remember(provider, onSelect) { { onSelect(provider) } }
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
        icon = R.drawable.ic_dns_24,
        text = provider.title,
        subtitle = subtitle,
        trailing = trailingContent,
        onClick = onClick,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun DohProviderItemPreview() = SettingsPreview {
    DohProviderItem(
        provider = DohProvider.CLOUDFLARE,
        isSelected = true,
        subtitle = DohProvider.CLOUDFLARE.description,
        radioColors = androidx.compose.material3.RadioButtonDefaults.colors(),
        onSelect = {}
    )
}
