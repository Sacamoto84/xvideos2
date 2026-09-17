package com.client.xvideos.screenSettings.section

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.R
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.common.net.doh.DohProvider
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.screenSettings.components.SettingsSwitchRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun NetworkSettingsSection() {
    val dohEnabled = Settings.doh_enabled.field.collectAsStateWithLifecycle().value
    val providerName = Settings.doh_provider.field.collectAsStateWithLifecycle().value
    val customUrl = Settings.doh_custom_url.field.collectAsStateWithLifecycle().value
    val fallbackToSystem = Settings.doh_fallback_to_system.field.collectAsStateWithLifecycle().value
    val ipv4Only = Settings.doh_ipv4_only.field.collectAsStateWithLifecycle().value

    val currentProvider = DohProvider.fromNameOrDefault(providerName)
    var showCustomUrlDialog by remember { mutableStateOf(false) }

    SettingsSectionTitle("DNS-over-HTTPS (DoH)")
    SettingsGroup {
        SettingsSwitchRow(
            icon = R.drawable.ic_dns_24,
            text = "DNS-over-HTTPS",
            subtitle = if (dohEnabled) {
                "Шифрование DNS и обход блокировок включены"
            } else {
                "Выключено (используется системный DNS)"
            },
            value = dohEnabled,
            onValueChange = {
                Settings.doh_enabled.setValue(it)
                AppDns.clearCache()
                if (it) {
                    SnackBar.success("DNS-over-HTTPS активирован")
                } else {
                    SnackBar.info("DoH выключен: активен системный DNS")
                }
            }
        )
    }

    if (dohEnabled) {
        DohProviderSelectionGroup(
            currentProvider = currentProvider,
            customUrl = customUrl,
            onSelectProvider = { provider ->
                Settings.doh_provider.setValue(provider.name)
                AppDns.clearCache()
                if (provider == DohProvider.CUSTOM && customUrl.isBlank()) {
                    showCustomUrlDialog = true
                }
            },
            onOpenCustomUrlDialog = { showCustomUrlDialog = true }
        )

        NetworkParamsGroup(
            fallbackToSystem = fallbackToSystem,
            ipv4Only = ipv4Only
        )

        DohDiagnosticsGroup()
    }

    if (showCustomUrlDialog) {
        CustomDohUrlDialog(
            initialUrl = customUrl,
            onDismiss = { showCustomUrlDialog = false },
            onSave = { newUrl ->
                Settings.doh_custom_url.setValue(newUrl)
                AppDns.clearCache()
                showCustomUrlDialog = false
                SnackBar.success("DoH URL сохранён")
            }
        )
    }
}

@Composable
private fun DohProviderSelectionGroup(
    currentProvider: DohProvider,
    customUrl: String,
    onSelectProvider: (DohProvider) -> Unit,
    onOpenCustomUrlDialog: () -> Unit
) {
    SettingsSectionTitle("Провайдер DNS")
    SettingsGroup {
        DohProvider.entries.forEachIndexed { index, provider ->
            if (index > 0) SettingsDivider()

            val subtitle = if (provider == DohProvider.CUSTOM) {
                if (customUrl.isNotBlank()) customUrl else provider.description
            } else {
                provider.description
            }

            SettingsListItem(
                icon = R.drawable.ic_dns_24,
                text = provider.title,
                subtitle = subtitle,
                trailing = {
                    RadioButton(
                        selected = currentProvider == provider,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = SettingsAccentColor,
                            unselectedColor = Color(0xFF938F99)
                        )
                    )
                },
                onClick = { onSelectProvider(provider) }
            )
        }

        if (currentProvider == DohProvider.CUSTOM) {
            SettingsDivider()
            SettingsListItem(
                icon = R.drawable.ic_dns_24,
                text = "Адрес DoH сервера",
                subtitle = if (customUrl.isNotBlank()) customUrl else "Нажмите для ввода URL",
                onClick = onOpenCustomUrlDialog
            )
        }
    }
}

@Composable
private fun NetworkParamsGroup(
    fallbackToSystem: Boolean,
    ipv4Only: Boolean
) {
    SettingsSectionTitle("Параметры сети")
    SettingsGroup {
        SettingsSwitchRow(
            icon = R.drawable.ic_dns_24,
            text = "Fallback на системный DNS",
            subtitle = if (fallbackToSystem) {
                "При сбое DoH запрос отправится через DNS оператора"
            } else {
                "Строгая изоляция: запросы только через DoH"
            },
            value = fallbackToSystem,
            onValueChange = { Settings.doh_fallback_to_system.setValue(it) }
        )
        SettingsDivider()
        SettingsSwitchRow(
            icon = R.drawable.ic_dns_24,
            text = "Только IPv4",
            subtitle = if (ipv4Only) {
                "Отключает задержки IPv6, ускоряет запуск видео"
            } else {
                "Разрешены IPv4 и IPv6 адреса"
            },
            value = ipv4Only,
            onValueChange = {
                Settings.doh_ipv4_only.setValue(it)
                AppDns.clearCache()
            }
        )
    }
}

@Composable
private fun DohDiagnosticsGroup() {
    val scope = rememberCoroutineScope()
    SettingsSectionTitle("Диагностика")
    SettingsGroup {
        SettingsListItem(
            icon = R.drawable.diagnostics_24,
            text = "Проверить DNS-резолвинг",
            subtitle = "Тестовый замер скорости отклика DoH-сервера",
            onClick = {
                scope.launch {
                    SnackBar.info("Тестирование соединения...")
                    val result = withContext(Dispatchers.IO) {
                        AppDns.diagnose("api.redgifs.com")
                    }
                    result.fold(
                        onSuccess = { diag ->
                            val ipText = diag.addresses.firstOrNull() ?: "нет IP"
                            SnackBar.success("${diag.providerTitle}: ${diag.elapsedMs} мс ($ipText)")
                        },
                        onFailure = { err ->
                            SnackBar.error("Ошибка DNS: ${err.message}")
                        }
                    )
                }
            }
        )
        SettingsDivider()
        SettingsListItem(
            icon = R.drawable.hard_disk_24,
            text = "Очистить DNS-кэш",
            subtitle = "Сброс всех закэшированных IP-адресов",
            onClick = {
                AppDns.clearCache()
                SnackBar.success("DNS-кэш успешно очищен")
            }
        )
    }
}

@Composable
private fun CustomDohUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tempUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    LavenderDialog(
        title = "Пользовательский DoH URL",
        onDismiss = onDismiss,
        confirmText = "Сохранить",
        onConfirm = { onSave(tempUrl.trim()) },
        content = {
            Text(
                "Введите HTTPS URL эндпоинта DoH резолвера (поддерживаются серверы с JSON API, RFC 8427):",
                style = Theme.L.Type.caption,
                color = Theme.L.grey1
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tempUrl,
                onValueChange = { tempUrl = it },
                placeholder = { Text("https://dns.example.com/dns-query") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun NetworkSettingsSectionPreview() = SettingsPreview {
    NetworkSettingsSection()
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun CustomDohUrlDialogPreview() = SettingsPreview {
    CustomDohUrlDialog(
        initialUrl = "https://dns.google/dns-query",
        onDismiss = {},
        onSave = {}
    )
}
