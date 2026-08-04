package com.client.xvideos.common.traficStatistic
// NetworkTrafficMonitor.kt - Сервис для мониторинга трафика
import android.net.TrafficStats
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

data class TrafficData(
    val downloadSpeed: Long = 0L, // байт/с
    val uploadSpeed: Long = 0L, // байт/с
    val totalDownloaded: Long = 0L, // всего скачано
    val totalUploaded: Long = 0L, // всего загружено
    val sessionDownloaded: Long = 0L, // за сессию скачано
    val sessionUploaded: Long = 0L, // за сессию загружено
    val isSupported: Boolean = true
)

@Singleton
class NetworkTrafficMonitor @Inject constructor() {

    val timeout = 2000L

    private val appUid = android.os.Process.myUid()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var sessionStartRxBytes = 0L
    private var sessionStartTxBytes = 0L
    private var previousRxBytes = 0L
    private var previousTxBytes = 0L
    private var previousTime = 0L

    private val _trafficFlow = MutableStateFlow(TrafficData())
    val trafficFlow: StateFlow<TrafficData> = _trafficFlow.asStateFlow()

    private var monitoringJob: Job? = null

    init {
        initializeCounters()
    }

    private fun initializeCounters() {
        val currentRxBytes = TrafficStats.getUidRxBytes(appUid)
        val currentTxBytes = TrafficStats.getUidTxBytes(appUid)

        if (currentRxBytes != TrafficStats.UNSUPPORTED.toLong() &&
            currentTxBytes != TrafficStats.UNSUPPORTED.toLong()) {

            sessionStartRxBytes = currentRxBytes
            sessionStartTxBytes = currentTxBytes
            previousRxBytes = currentRxBytes
            previousTxBytes = currentTxBytes
            previousTime = System.currentTimeMillis()

            _trafficFlow.value = _trafficFlow.value.copy(
                totalDownloaded = currentRxBytes,
                totalUploaded = currentTxBytes,
                isSupported = true
            )
        } else {
            _trafficFlow.value = _trafficFlow.value.copy(isSupported = false)
        }
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = scope.launch {
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(calculateTrafficData())
                    delay(timeout) // Обновление каждую 2 секунду
                }
            }
                .flowOn(Dispatchers.IO)
                .collect { trafficData ->
                    _trafficFlow.value = trafficData
                }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun calculateTrafficData(): TrafficData {
        val currentRxBytes = TrafficStats.getUidRxBytes(appUid)
        val currentTxBytes = TrafficStats.getUidTxBytes(appUid)
        val currentTime = System.currentTimeMillis()

        if (currentRxBytes == TrafficStats.UNSUPPORTED.toLong() ||
            currentTxBytes == TrafficStats.UNSUPPORTED.toLong()) {
            return _trafficFlow.value.copy(isSupported = false)
        }

        val timeDiff = (currentTime - previousTime) / timeout.toFloat()

        val downloadSpeed = if (timeDiff > 0 && previousRxBytes > 0) {
            ((currentRxBytes - previousRxBytes) / timeDiff).toLong().coerceAtLeast(0L)
        } else 0L

        val uploadSpeed = if (timeDiff > 0 && previousTxBytes > 0) {
            ((currentTxBytes - previousTxBytes) / timeDiff).toLong().coerceAtLeast(0L)
        } else 0L

        // Обновляем предыдущие значения
        previousRxBytes = currentRxBytes
        previousTxBytes = currentTxBytes
        previousTime = currentTime

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

    fun resetSession() {
        scope.launch {
            val currentRxBytes = TrafficStats.getUidRxBytes(appUid)
            val currentTxBytes = TrafficStats.getUidTxBytes(appUid)

            if (currentRxBytes != TrafficStats.UNSUPPORTED.toLong() &&
                currentTxBytes != TrafficStats.UNSUPPORTED.toLong()) {

                sessionStartRxBytes = currentRxBytes
                sessionStartTxBytes = currentTxBytes
            }
        }
    }

    fun destroy() {
        stopMonitoring()
        scope.cancel()
    }
}
