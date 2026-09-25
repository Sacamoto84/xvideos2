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

private val RADIO_UNSELECTED_COLOR = Color(0xFF938F99)
private const val DIAGNOSTIC_TEST_HOST = "api.redgifs.com"

private const val TITLE_DOH = "DNS-over-HTTPS (DoH)"
private const val TEXT_DOH = "DNS-over-HTTPS"
private const val SUBTITLE_DOH_ENABLED = "Шифрование DNS и обход блокировок включены"
private const val SUBTITLE_DOH_DISABLED = "Выключено (используется системный DNS)"
private const val MSG_DOH_ACTIVATED = "DNS-over-HTTPS активирован"
private const val MSG_DOH_DEACTIVATED = "DoH выключен: активен системный DNS"
private const val MSG_URL_SAVED = "DoH URL сохранён"
private const val TITLE_PROVIDER = "Провайдер DNS"
private const val TEXT_DOH_SERVER_URL = "Адрес DoH сервера"
private const val HINT_ENTER_URL = "Нажмите для ввода URL"
private const val TITLE_NETWORK_PARAMS = "Параметры сети"
private const val TEXT_FALLBACK = "Fallback на системный DNS"
private const val SUBTITLE_FALLBACK_ENABLED = "При сбое DoH запрос отправится через DNS оператора"
private const val SUBTITLE_FALLBACK_DISABLED = "Строгая изоляция: запросы только через DoH"
private const val TEXT_IPV4_ONLY = "Только IPv4"
private const val SUBTITLE_IPV4_ENABLED = "Отключает задержки IPv6, ускоряет запуск видео"
private const val SUBTITLE_IPV4_DISABLED = "Разрешены IPv4 и IPv6 адреса"
private const val TITLE_DIAGNOSTICS = "Диагностика"
private const val TEXT_CHECK_DNS = "Проверить DNS-резолвинг"
private const val SUBTITLE_CHECK_DNS = "Тестовый замер скорости отклика DoH-сервера"
private const val TEXT_CLEAR_CACHE = "Очистить DNS-кэш"
private const val SUBTITLE_CLEAR_CACHE = "Сброс всех закэшированных IP-адресов"
private const val MSG_TESTING_CONNECTION = "Тестирование соединения..."
private const val MSG_NO_IP = "нет IP"
private const val MSG_DNS_ERROR_PREFIX = "Ошибка DNS: "
private const val MSG_CACHE_CLEARED = "DNS-кэш успешно очищен"
private const val TITLE_CUSTOM_URL_DIALOG = "Пользовательский DoH URL"
private const val BUTTON_SAVE = "Сохранить"
private const val TEXT_CUSTOM_URL_DESCRIPTION = "Введите HTTPS URL эндпоинта DoH резолвера (поддерживаются серверы с JSON API, RFC 8427):"
private const val PLACEHOLDER_URL = "https://dns.example.com/dns-query"
private val DIALOG_SPACER_HEIGHT = 8.dp

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
            SnackBar.success(MSG_URL_SAVED)
        }
    }

    val onToggleDoh: (Boolean) -> Unit = remember {
        { enabled ->
            Settings.doh_enabled.setValue(enabled)
            AppDns.clearCache()
            if (enabled) {
                SnackBar.success(MSG_DOH_ACTIVATED)
            } else {
                SnackBar.info(MSG_DOH_DEACTIVATED)
            }
        }
    }

    val dohSubtitle = remember(dohEnabled) {
        if (dohEnabled) {
            SUBTITLE_DOH_ENABLED
        } else {
            SUBTITLE_DOH_DISABLED
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle(TITLE_DOH)
        SettingsGroup {
            SettingsSwitchRow(
                icon = R.drawable.ic_dns_24,
                text = TEXT_DOH,
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
        if (customUrl.isNotBlank()) customUrl else HINT_ENTER_URL
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle(TITLE_PROVIDER)
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
                        onSelect = onSelectProvider
                    )
                }
            }

            if (currentProvider == DohProvider.CUSTOM) {
                SettingsDivider()
                SettingsListItem(
                    icon = R.drawable.ic_dns_24,
                    text = TEXT_DOH_SERVER_URL,
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
    onSelect: (DohProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = remember(provider, onSelect) { { onSelect(provider) } }
    val trailingContent: @Composable () -> Unit = remember(isSelected) {
        {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = SettingsAccentColor,
                    unselectedColor = RADIO_UNSELECTED_COLOR
                )
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

    val fallbackSubtitle = remember(fallbackToSystem) {
        if (fallbackToSystem) {
            SUBTITLE_FALLBACK_ENABLED
        } else {
            SUBTITLE_FALLBACK_DISABLED
        }
    }
    val ipv4OnlySubtitle = remember(ipv4Only) {
        if (ipv4Only) {
            SUBTITLE_IPV4_ENABLED
        } else {
            SUBTITLE_IPV4_DISABLED
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle(TITLE_NETWORK_PARAMS)
        SettingsGroup {
            SettingsSwitchRow(
                icon = R.drawable.ic_dns_24,
                text = TEXT_FALLBACK,
                subtitle = fallbackSubtitle,
                value = fallbackToSystem,
                onValueChange = onFallbackChange
            )
            SettingsDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_dns_24,
                text = TEXT_IPV4_ONLY,
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
                SnackBar.info(MSG_TESTING_CONNECTION)
                val result = withContext(Dispatchers.IO) {
                    AppDns.diagnose(DIAGNOSTIC_TEST_HOST)
                }
                result.fold(
                    onSuccess = { diag ->
                        val ipText = diag.addresses.firstOrNull() ?: MSG_NO_IP
                        SnackBar.success("${diag.providerTitle}: ${diag.elapsedMs} мс ($ipText)")
                    },
                    onFailure = { err ->
                        SnackBar.error("$MSG_DNS_ERROR_PREFIX${err.message}")
                    }
                )
            }
        }
    }
    val onClearCache = remember {
        {
            AppDns.clearCache()
            SnackBar.success(MSG_CACHE_CLEARED)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionTitle(TITLE_DIAGNOSTICS)
        SettingsGroup {
            SettingsListItem(
                icon = R.drawable.diagnostics_24,
                text = TEXT_CHECK_DNS,
                subtitle = SUBTITLE_CHECK_DNS,
                onClick = onDiagnose
            )
            SettingsDivider()
            SettingsListItem(
                icon = R.drawable.hard_disk_24,
                text = TEXT_CLEAR_CACHE,
                subtitle = SUBTITLE_CLEAR_CACHE,
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

    LavenderDialog(
        title = TITLE_CUSTOM_URL_DIALOG,
        onDismiss = onDismiss,
        confirmText = BUTTON_SAVE,
        onConfirm = onConfirmSave,
        content = {
            Text(
                TEXT_CUSTOM_URL_DESCRIPTION,
                style = Theme.L.Type.caption,
                color = Theme.L.grey1
            )
            Spacer(Modifier.height(DIALOG_SPACER_HEIGHT))
            OutlinedTextField(
                value = tempUrl,
                onValueChange = onUrlChange,
                placeholder = { Text(PLACEHOLDER_URL) },
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
