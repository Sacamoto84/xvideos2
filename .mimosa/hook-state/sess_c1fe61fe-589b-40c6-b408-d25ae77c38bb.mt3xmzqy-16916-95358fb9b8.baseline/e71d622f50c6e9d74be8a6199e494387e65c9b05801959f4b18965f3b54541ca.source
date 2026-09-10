package com.client.xvideos.common.traficStatistic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Доступ к [NetworkTrafficMonitor] из composable вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkTrafficMonitorEntryPoint {
    fun networkTrafficMonitor(): NetworkTrafficMonitor
}

/**
 * Поток статистики трафика для виджетов базового слоя.
 *
 * Раньше монитор доставался из `NetworkTrafficMonitor.current` — статического
 * слота, который заполнял `App.onCreate`. Слот был не нужен: класс и так
 * `@Singleton` с `@Inject`-конструктором, просто composable не умели его
 * попросить.
 *
 * В Compose Preview DI-графа нет, поэтому там отдаётся пустой поток — иначе
 * превью любого экрана со счётчиком трафика падало бы.
 */
@Composable
fun rememberTrafficFlow(): StateFlow<TrafficData> {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    return remember(context, inPreview) {
        if (inPreview) {
            MutableStateFlow(TrafficData())
        } else {
            EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    NetworkTrafficMonitorEntryPoint::class.java,
                )
                .networkTrafficMonitor()
                .trafficFlow
        }
    }
}
