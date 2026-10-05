package com.client.xvideos.screenSettings.section

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.R
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.common.net.doh.DohProvider
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.screenSettings.components.SettingsSwitchRow
import com.client.xvideos.common.net.doh.parseDohUrl
import com.client.xvideos.screenSettings.molecule.CustomDohUrlDialog
import com.client.xvideos.screenSettings.molecule.DohDiagnosticsGroup
import com.client.xvideos.screenSettings.molecule.DohProviderSelectionGroup
import com.client.xvideos.screenSettings.molecule.NetworkParamsGroup

@Composable
internal fun NetworkSettingsSection(
    modifier: Modifier = Modifier,
) {
    val dohEnabled by Settings.doh_enabled.field.collectAsStateWithLifecycle()
    val providerName by Settings.doh_provider.field.collectAsStateWithLifecycle()
    val customUrl by Settings.doh_custom_url.field.collectAsStateWithLifecycle()
    val fallbackToSystem by Settings.doh_fallback_to_system.field.collectAsStateWithLifecycle()
    val ipv4Only by Settings.doh_ipv4_only.field.collectAsStateWithLifecycle()

    val currentProvider = remember(providerName) { DohProvider.fromNameOrDefault(providerName) }
    var showCustomUrlDialog by remember { mutableStateOf(false) }

    val onDismissCustomUrl = remember { { showCustomUrlDialog = false } }
    val onOpenCustomUrl = remember { { showCustomUrlDialog = true } }

    BackHandler(enabled = showCustomUrlDialog, onBack = onDismissCustomUrl)

    val onSelectProvider: (DohProvider) -> Unit = remember(customUrl) {
        { provider ->
            // «Свой» без адреса провайдером не становится: сначала адрес.
            // Раньше провайдер менялся сразу, и закрытый без ввода диалог
            // оставлял «Свой» выбранным без адреса.
            if (provider == DohProvider.CUSTOM && parseDohUrl(customUrl).isFailure) {
                showCustomUrlDialog = true
            } else {
                Settings.doh_provider.setValue(provider.name)
                AppDns.clearCache()
            }
        }
    }

    // Диалог отдаёт сюда уже проверенный адрес.
    val onSaveCustomUrl: (String) -> Unit = remember {
        { newUrl ->
            Settings.doh_custom_url.setValue(newUrl)
            Settings.doh_provider.setValue(DohProvider.CUSTOM.name)
            AppDns.clearCache()
            showCustomUrlDialog = false
            SnackBar.success("DoH URL сохранён")
        }
    }

    val onToggleDoh: (Boolean) -> Unit = remember {
        { enabled ->
            Settings.doh_enabled.setValue(enabled)
            AppDns.clearCache()
            if (enabled) {
                SnackBar.success("DNS-over-HTTPS активирован")
            } else {
                SnackBar.info("DoH выключен: активен системный DNS")
            }
        }
    }

    val dohSubtitle = if (dohEnabled) {
        "Шифрование DNS и обход блокировок включены"
    } else {
        "Выключено (используется системный DNS)"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle("DNS-over-HTTPS (DoH)")
        SettingsGroup {
            SettingsSwitchRow(
                icon = R.drawable.ic_dns_24,
                text = "DNS-over-HTTPS",
                subtitle = dohSubtitle,
                value = dohEnabled,
                onValueChange = onToggleDoh
            )
        }

        if (dohEnabled) {
            DohProviderSelectionGroup(
                currentProvider = currentProvider,
                customUrl = customUrl,
                onSelectProvider = onSelectProvider,
                onOpenCustomUrlDialog = onOpenCustomUrl
            )

            NetworkParamsGroup(
                fallbackToSystem = fallbackToSystem,
                ipv4Only = ipv4Only
            )

            DohDiagnosticsGroup()
        }
    }

    if (showCustomUrlDialog) {
        CustomDohUrlDialog(
            initialUrl = customUrl,
            onDismiss = onDismissCustomUrl,
            onSave = onSaveCustomUrl
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun NetworkSettingsSectionPreview() = SettingsPreview {
    NetworkSettingsSection()
}
