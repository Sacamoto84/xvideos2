package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.imports.BundleImporter
import com.client.xvideos.common.p2p.nearby.NearbyClient
import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Состояние экрана приёма. */
sealed interface ReceiveState {
    data object Idle : ReceiveState
    data object Advertising : ReceiveState
    data class Connecting(val endpointName: String, val authDigits: String) : ReceiveState
    data class Receiving(val transferred: Long, val total: Long) : ReceiveState
    data object Done : ReceiveState
    data class Error(val message: String) : ReceiveState
}

/**
 * Приёмная сторона: рекламируется, принимает соединение, буферизует payload'ы,
 * по приходу манифеста и всех файлов вызывает [importer].
 */
class P2pReceiveController(
    private val nearby: NearbyClient,
    private val importer: BundleImporter,
    private val scope: CoroutineScope,
    private val deviceName: String,
) {
    private val _state = MutableStateFlow<ReceiveState>(ReceiveState.Idle)
    val state: StateFlow<ReceiveState> = _state.asStateFlow()

    private val receivedFiles = ConcurrentHashMap<Long, File>()
    private val importMutex = Mutex()
    @Volatile
    private var importCompleted = false
    @Volatile
    private var manifest: P2pManifest? = null
    @Volatile
    private var currentEndpoint: String? = null

    /** Имя телефона-отправителя текущей/последней сессии — для уведомлений. */
    var peerName: String = DEFAULT_PEER_NAME
        private set
    private var eventsJob: Job? = null
    private var pendingStopJob: Job? = null
    private var disconnectGraceJob: Job? = null

    fun start() {
        Timber.d("P2P Receiver: Starting advertising...")
        pendingStopJob?.cancel()
        pendingStopJob = null
        disconnectGraceJob?.cancel()
        disconnectGraceJob = null
        importCompleted = false
        receivedFiles.clear()
        manifest = null
        currentEndpoint = null
        _state.value = ReceiveState.Advertising
        eventsJob?.cancel()
        eventsJob = scope.launch { nearby.events.collect { handle(it) } }
        nearby.startAdvertising(deviceName)
    }

    private suspend fun handle(event: P2pEvent) {
        Timber.d("P2P Receiver: handle event $event")
        when (event) {
            is P2pEvent.ConnectionInitiated -> {
                Timber.d("P2P Receiver: Connection initiated from ${event.endpointName} (id=${event.endpointId}, auth=${event.authenticationDigits}). Waiting for confirmation.")
                currentEndpoint = event.endpointId
                peerName = event.endpointName
                _state.value = ReceiveState.Connecting(event.endpointName, event.authenticationDigits)
            }
            is P2pEvent.Connected -> {
                Timber.d("P2P Receiver: Connected to $currentEndpoint")
                nearby.stopAdvertising() // Останавливаем рекламу после подключения для стабильности
                _state.value = ReceiveState.Receiving(0, 0)
            }
            is P2pEvent.TransferProgress ->
                // GMS шлёт статусы payload'ов и после завершения сессии —
                // не затираем Advertising/Done поздним прогрессом.
                if (_state.value is ReceiveState.Receiving) {
                    _state.value = ReceiveState.Receiving(event.transferred, event.total)
                }
            is P2pEvent.FilePayloadReceived -> {
                receivedFiles[event.payloadId] = event.file
                tryImport()
            }
            is P2pEvent.BytesPayloadReceived -> {
                manifest = runCatching { P2pManifestCodec.fromBytes(event.bytes) }.getOrNull()
                if (manifest == null) _state.value = ReceiveState.Error("Битый манифест") else tryImport()
            }
            is P2pEvent.Disconnected -> {
                // Ошибка только если разрыв случился посреди активной сессии,
                // и не пришло завершение копирования FILE-payload в кеш.
                if (_state.value is ReceiveState.Connecting || _state.value is ReceiveState.Receiving) {
                    disconnectGraceJob?.cancel()
                    disconnectGraceJob = scope.launch {
                        delay(DISCONNECT_GRACE_MS)
                        if (shouldFailOnDisconnect()) {
                            _state.value = ReceiveState.Error("Соединение разорвано")
                        }
                    }
                }
            }
            is P2pEvent.Failed -> _state.value = ReceiveState.Error(event.message)
            // Недоставленный payload раньше проваливался в `else` и терялся:
            // приём просто ничего не импортировал, молча и без следа на экране.
            is P2pEvent.PayloadTransferFailed -> {
                Timber.w("P2P Receiver: payload ${event.payloadId} не доставлен")
                _state.value = ReceiveState.Error("Файл не получен целиком")
            }
            else -> Unit
        }
    }

    private fun shouldFailOnDisconnect(): Boolean {
        val currentManifest = manifest
        val allArrived = currentManifest != null &&
            currentManifest.files.all { receivedFiles.containsKey(it.payloadId) }
        val awaitingData = _state.value is ReceiveState.Connecting || _state.value is ReceiveState.Receiving
        return !importCompleted && !allArrived && awaitingData
    }

    private suspend fun tryImport() {
        importMutex.withLock {
            if (importCompleted) return
            val m = manifest ?: return
            if (!m.files.all { receivedFiles.containsKey(it.payloadId) }) return
            disconnectGraceJob?.cancel()
            disconnectGraceJob = null
            try {
                Timber.d("P2P Receiver: All files received, importing bundle...")
                withContext(Dispatchers.IO) {
                    importer.import(m, receivedFiles.toMap())
                }
                importCompleted = true
                _state.value = ReceiveState.Done
                // Не вызываем stopAll сразу, даем время отправителю получить подтверждение.
                pendingStopJob = scope.launch {
                    delay(2000)
                    if (_state.value is ReceiveState.Done) nearby.stopAll()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "P2P Receiver: Import failed")
                _state.value = ReceiveState.Error(e.message ?: "Ошибка импорта")
            }
        }
    }

    fun accept() {
        currentEndpoint?.let {
            Timber.d("P2P Receiver: Manual accept for $it")
            nearby.acceptConnection(it)
        }
    }

    fun reject() {
        currentEndpoint?.let { nearby.rejectConnection(it) }
        nearby.stopAll()
        _state.value = ReceiveState.Idle
    }

    fun stop() {
        eventsJob?.cancel()
        eventsJob = null
        pendingStopJob?.cancel()
        pendingStopJob = null
        disconnectGraceJob?.cancel()
        disconnectGraceJob = null
        importCompleted = false
        nearby.stopAll()
        _state.value = ReceiveState.Idle
    }

    private companion object {
        const val DEFAULT_PEER_NAME = "Устройство"
        const val DISCONNECT_GRACE_MS = 1200L
    }
}
