package com.client.xvideos.screenSettings.section

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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

private const val ICON_DNS = R.drawable.ic_dns_24

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
            Settings.doh_provider.setValue(provider.name)
            AppDns.clearCache()
            if (provider == DohProvider.CUSTOM && customUrl.isBlank()) {
                showCustomUrlDialog = true
            }
        }
    }

    val onSaveCustomUrl: (String) -> Unit = remember {
        { newUrl ->
            Settings.doh_custom_url.setValue(newUrl)
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
                icon = ICON_DNS,
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

@Composable
private fun DohProviderSelectionGroup(
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
                    icon = ICON_DNS,
                    text = "Адрес DoH сервера",
                    subtitle = customSubtitle,
                    onClick = onOpenCustomUrlDialog
                )
            }
        }
    }
}

@Composable
private fun DohProviderItem(
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
        icon = ICON_DNS,
        text = provider.title,
        subtitle = subtitle,
        trailing = trailingContent,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun NetworkParamsGroup(
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
                icon = ICON_DNS,
                text = "Fallback на системный DNS",
                subtitle = fallbackSubtitle,
                value = fallbackToSystem,
                onValueChange = onFallbackChange
            )
            SettingsDivider()
            SettingsSwitchRow(
                icon = ICON_DNS,
                text = "Только IPv4",
                subtitle = ipv4OnlySubtitle,
                value = ipv4Only,
                onValueChange = onIpv4OnlyChange
            )
        }
    }
}

@Composable
private fun DohDiagnosticsGroup(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val onDiagnose: () -> Unit = remember(scope) {
        {
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
    }
    val onClearCache = remember {
        {
            AppDns.clearCache()
            SnackBar.success("DNS-кэш успешно очищен")
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle("Диагностика")
        SettingsGroup {
            SettingsListItem(
                icon = R.drawable.diagnostics_24,
                text = "Проверить DNS-резолвинг",
                subtitle = "Тестовый замер скорости отклика DoH-сервера",
                onClick = onDiagnose
            )
            SettingsDivider()
            SettingsListItem(
                icon = R.drawable.hard_disk_24,
                text = "Очистить DNS-кэш",
                subtitle = "Сброс всех закэшированных IP-адресов",
                onClick = onClearCache
            )
        }
    }
}

@Composable
private fun CustomDohUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tempUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    val onConfirmSave: () -> Unit = remember(onSave) { { onSave(tempUrl.trim()) } }
    val onUrlChange: (String) -> Unit = remember { { newUrl -> tempUrl = newUrl } }
    val placeholderContent: @Composable () -> Unit = remember {
        { Text("https://dns.example.com/dns-query") }
    }
    val dialogContent: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit =
        remember(tempUrl, onUrlChange, placeholderContent) {
            {
                Text(
                    "Введите HTTPS URL эндпоинта DoH резолвера (поддерживаются серверы с JSON API, RFC 8427):",
                    style = Theme.L.Type.caption,
                    color = Theme.L.grey1
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = onUrlChange,
                    placeholder = placeholderContent,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

    LavenderDialog(
        title = "Пользовательский DoH URL",
        onDismiss = onDismiss,
        confirmText = "Сохранить",
        onConfirm = onConfirmSave,
        content = dialogContent
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
