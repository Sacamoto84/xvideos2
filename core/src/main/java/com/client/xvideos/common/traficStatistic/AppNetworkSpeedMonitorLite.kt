package com.client.xvideos.common.traficStatistic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppNetworkSpeedMonitorLite() {
    val trafficFlow = rememberTrafficFlow()
    val trafficData by trafficFlow.collectAsStateWithLifecycle()

    AppNetworkSpeedMonitorLiteContent(
        downloadSpeed = trafficData.downloadSpeed,
        sessionDownloaded = trafficData.sessionDownloaded
    )
}
