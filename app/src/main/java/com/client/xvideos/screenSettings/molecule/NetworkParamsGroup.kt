package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.screenSettings.components.SettingsSwitchRow

@Composable
internal fun NetworkParamsGroup(
    fallbackToSystem: Boolean,
    ipv4Only: Boolean,
    modifier: Modifier = Modifier,
) {
    val onFallbackChange: (Boolean) -> Unit = remember {
        { enabled -> Settings.doh_fallback_to_system.setValue(enabled) }
    }
    val onIpv4OnlyChange: (Boolean) -> Unit = remember {
        { enabled ->
            Settings.doh_ipv4_only.setValue(enabled)
            AppDns.clearCache()
        }
    }

    val fallbackSubtitle = if (fallbackToSystem) {
        "При сбое DoH запрос отправится через DNS оператора"
    } else {
        "Строгая изоляция: запросы только через DoH"
    }
    val ipv4OnlySubtitle = if (ipv4Only) {
        "Отключает задержки IPv6, ускоряет запуск видео"
    } else {
        "Разрешены IPv4 и IPv6 адреса"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle("Параметры сети")
        SettingsGroup {
            SettingsSwitchRow(
                icon = R.drawable.ic_dns_24,
                text = "Fallback на системный DNS",
                subtitle = fallbackSubtitle,
                value = fallbackToSystem,
                onValueChange = onFallbackChange
            )
            SettingsDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_dns_24,
                text = "Только IPv4",
                subtitle = ipv4OnlySubtitle,
                value = ipv4Only,
                onValueChange = onIpv4OnlyChange
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun NetworkParamsGroupPreview() {
    NetworkParamsGroup(fallbackToSystem = true, ipv4Only = true)
}
