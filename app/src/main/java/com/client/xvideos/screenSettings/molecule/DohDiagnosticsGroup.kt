package com.client.xvideos.screenSettings.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.net.doh.AppDns
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.repository.LusciousEndpoints
import com.client.xvideos.r.network.http.Route
import com.client.xvideos.screenSettings.components.SettingsDivider
import com.client.xvideos.screenSettings.components.SettingsGroup
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview
import com.client.xvideos.screenSettings.components.SettingsSectionTitle
import com.client.xvideos.x.urlStart
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Серверы разделов, имена которых проверяет диагностика DNS: подпись раздела и имя сервера. */
private val DIAGNOSED_HOSTS: List<Pair<String, String>> = listOf(
    "X" to urlStart,
    "L" to LusciousEndpoints.API_ANONYMOUS,
    "R" to Route.BASE,
).mapNotNull { (section, url) -> runCatching { URI(url).host }.getOrNull()?.let { section to it } }

/**
 * Диагностика DNS: проверяет имена серверов всех трёх разделов. Раньше
 * проверялся один зашитый адрес R: провайдер, который отдаёт его и не отдаёт
 * адреса X или L, показывал успех при двух неработающих разделах.
 */
@Composable
internal fun DohDiagnosticsGroup(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val onDiagnose: () -> Unit = remember(scope) {
        {
            scope.launch {
                SnackBar.info("Тестирование соединения...")
                val results = withContext(Dispatchers.IO) {
                    DIAGNOSED_HOSTS.map { (section, host) -> section to AppDns.diagnose(host) }
                }
                val provider = results.firstNotNullOfOrNull { it.second.getOrNull()?.providerTitle }
                val summary = results.joinToString(", ") { (section, result) ->
                    result.fold(
                        onSuccess = { "$section ${it.elapsedMs} мс" },
                        onFailure = { "$section не найден" },
                    )
                }
                val text = if (provider != null) "$provider: $summary" else "Ошибка DNS: $summary"
                if (results.all { it.second.isSuccess }) SnackBar.success(text) else SnackBar.error(text)
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
                subtitle = "Имена серверов X, L и R через выбранный резолвер",
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

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun DohDiagnosticsGroupPreview() = SettingsPreview {
    DohDiagnosticsGroup()
}
