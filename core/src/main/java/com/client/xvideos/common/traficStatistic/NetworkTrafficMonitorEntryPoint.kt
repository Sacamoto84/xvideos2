package com.client.xvideos.common.traficStatistic

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Доступ к [NetworkTrafficMonitor] из composable вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkTrafficMonitorEntryPoint {
    fun networkTrafficMonitor(): NetworkTrafficMonitor
}
