package com.client.xvideos.common.traficStatistic

import android.net.TrafficStats
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Снимок метрик сетевого трафика приложения.
 *
 * @property downloadSpeed Мгновенная скорость скачивания в байтах в секунду (B/s).
 * @property uploadSpeed Мгновенная скорость отдачи в байтах в секунду (B/s).
 * @property totalDownloaded Общий объем входящего трафика за все время работы ОС (Rx).
 * @property totalUploaded Общий объем исходящего трафика за все время работы ОС (Tx).
 * @property sessionDownloaded Объем входящего трафика за текущую сессию работы приложения.
 * @property sessionUploaded Объем исходящего трафика за текущую сессию работы приложения.
 * @property isSupported Флаг поддержки системных счетчиков [TrafficStats] текущим ядром устройства.
 */
data class TrafficData(
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val totalDownloaded: Long = 0L,
    val totalUploaded: Long = 0L,
    val sessionDownloaded: Long = 0L,
    val sessionUploaded: Long = 0L,
    val isSupported: Boolean = true
) {
    /** Суммарный объем входящего и исходящего трафика за текущую сессию. */
    val totalSessionTraffic: Long get() = sessionDownloaded + sessionUploaded

    /** Суммарный объем входящего и исходящего трафика за все время. */
    val totalOverallTraffic: Long get() = totalDownloaded + totalUploaded

    /** Истина, если сетевой обмен отсутствует (нулевая скорость в обоих направлениях). */
    val isIdle: Boolean get() = downloadSpeed == 0L && uploadSpeed == 0L

    companion object {
        /** Пустой объект с нулевыми счетчиками. */
        val EMPTY = TrafficData()

        /** Объект для устройств без поддержки [TrafficStats]. */
        val UNSUPPORTED = TrafficData(isSupported = false)
    }
}

/**
 * Синглтон-сервис периодического мониторинга скорости и объема сети процесса приложения.
 *
 * Использует системный [TrafficStats.getUidRxBytes] для подсчета байтов конкретного UID процесса.
 * Предоставляет реактивный [StateFlow] для обновления UI индикаторов трафика без блокировки потоков.
 */
@Singleton
class NetworkTrafficMonitor @Inject constructor() {

    /** Интервал обновления метрик (2000 мс). */
    val timeout = 2000L

    private val appUid = android.os.Process.myUid()
    private val monitorDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(monitorDispatcher + SupervisorJob())

    private var countersInitialized = false
    private var sessionStartRxBytes = 0L
    private var sessionStartTxBytes = 0L
    private var previousRxBytes = 0L
    private var previousTxBytes = 0L
    private var previousTimeElapsedMs = 0L

    private val _trafficFlow = MutableStateFlow(TrafficData())

    /** Реактивный поток актуальных данных о трафике. */
    val trafficFlow: StateFlow<TrafficData> = _trafficFlow.asStateFlow()

    private var monitoringJob: Job? = null

    /**
     * Инициализация базовых значений счетчиков на рабочем потоке мониторинга.
     */
    private fun initializeCounters() {
        val currentRxBytes = TrafficStats.getUidRxBytes(appUid)
        val currentTxBytes = TrafficStats.getUidTxBytes(appUid)

        if (currentRxBytes != TrafficStats.UNSUPPORTED.toLong() &&
            currentTxBytes != TrafficStats.UNSUPPORTED.toLong()
        ) {
            if (!countersInitialized) {
                sessionStartRxBytes = currentRxBytes
                sessionStartTxBytes = currentTxBytes
                countersInitialized = true
            }
            previousRxBytes = currentRxBytes
            previousTxBytes = currentTxBytes
            previousTimeElapsedMs = SystemClock.elapsedRealtime()

            _trafficFlow.value = _trafficFlow.value.copy(
                totalDownloaded = currentRxBytes,
                totalUploaded = currentTxBytes,
                isSupported = true
            )
        } else {
            _trafficFlow.value = _trafficFlow.value.copy(isSupported = false)
        }
    }

    /**
     * Запускает фоновый цикл периодического замера скорости.
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = scope.launch {
            initializeCounters()
            while (isActive) {
                delay(timeout)
                _trafficFlow.value = calculateTrafficData()
            }
        }
    }

    /**
     * Приостанавливает цикл периодического замера скорости.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Вычисляет дельту байтов за прошедший монотонный интервал и определяет текущую скорость.
     */
    private fun calculateTrafficData(): TrafficData {
        val currentRxBytes = TrafficStats.getUidRxBytes(appUid)
        val currentTxBytes = TrafficStats.getUidTxBytes(appUid)
        val currentTime = SystemClock.elapsedRealtime()

        if (currentRxBytes == TrafficStats.UNSUPPORTED.toLong() ||
            currentTxBytes == TrafficStats.UNSUPPORTED.toLong()
        ) {
            return _trafficFlow.value.copy(isSupported = false)
        }

        val timeDiffSec = (currentTime - previousTimeElapsedMs) / 1000f

        val downloadSpeed = if (timeDiffSec > 0f && previousRxBytes > 0) {
            ((currentRxBytes - previousRxBytes) / timeDiffSec).toLong().coerceAtLeast(0L)
        } else 0L

        val uploadSpeed = if (timeDiffSec > 0f && previousTxBytes > 0) {
            ((currentTxBytes - previousTxBytes) / timeDiffSec).toLong().coerceAtLeast(0L)
        } else 0L

        previousRxBytes = currentRxBytes
        previousTxBytes = currentTxBytes
        previousTimeElapsedMs = currentTime

        return TrafficData(
            downloadSpeed = downloadSpeed,
            uploadSpeed = uploadSpeed,
            totalDownloaded = currentRxBytes,
            totalUploaded = currentTxBytes,
            sessionDownloaded = (currentRxBytes - sessionStartRxBytes).coerceAtLeast(0L),
            sessionUploaded = (currentTxBytes - sessionStartTxBytes).coerceAtLeast(0L),
            isSupported = true
        )
    }

    /**
     * Сбрасывает точку отсчета трафика для текущей сессии на текущие значения.
     */
    fun resetSession() {
        scope.launch {
            val currentRxBytes = TrafficStats.getUidRxBytes(appUid)
            val currentTxBytes = TrafficStats.getUidTxBytes(appUid)

            if (currentRxBytes != TrafficStats.UNSUPPORTED.toLong() &&
                currentTxBytes != TrafficStats.UNSUPPORTED.toLong()
            ) {
                sessionStartRxBytes = currentRxBytes
                sessionStartTxBytes = currentTxBytes
                countersInitialized = true
            }
        }
    }

    /**
     * Полное освобождение ресурсов и завершение скоупа монитора.
     */
    fun destroy() {
        stopMonitoring()
        scope.cancel()
    }
}
