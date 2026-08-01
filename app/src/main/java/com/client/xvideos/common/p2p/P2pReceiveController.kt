package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.imports.BundleImporter
import com.client.xvideos.common.p2p.nearby.NearbyClient
import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/** Состояние экрана приёма. */
sealed interface ReceiveState {
    data object Idle : ReceiveState
    data object Advertising : ReceiveState
    data class Connecting(val endpointName: String) : ReceiveState
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

    private val receivedFiles = mutableMapOf<Long, File>()
    private var manifest: P2pManifest? = null
    private var currentEndpoint: String? = null

    /** Имя телефона-отправителя текущей/последней сессии — для уведомлений. */
    var peerName: String = DEFAULT_PEER_NAME
        private set
    private var eventsJob: kotlinx.coroutines.Job? = null
    private var pendingStopJob: kotlinx.coroutines.Job? = null

    fun start() {
        Timber.d("P2P Receiver: Starting advertising...")
        pendingStopJob?.cancel()
        pendingStopJob = null
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
                Timber.d("P2P Receiver: Connection initiated from ${event.endpointName} (id=${event.endpointId}). Automatically accepting.")
                currentEndpoint = event.endpointId
                peerName = event.endpointName
                _state.value = ReceiveState.Connecting(event.endpointName)
                nearby.acceptConnection(event.endpointId)
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
            is P2pEvent.Disconnected ->
                // Ошибка только если разрыв случился посреди активной сессии.
                // Запоздавший disconnect от предыдущей сессии не должен ронять рекламу.
                if (_state.value is ReceiveState.Connecting || _state.value is ReceiveState.Receiving) {
                    _state.value = ReceiveState.Error("Соединение разорвано")
                }
            is P2pEvent.Failed -> _state.value = ReceiveState.Error(event.message)
            else -> Unit
        }
    }

    private suspend fun tryImport() {
        val m = manifest ?: return
        if (!m.files.all { receivedFiles.containsKey(it.payloadId) }) return
        try {
            Timber.d("P2P Receiver: All files received, importing bundle...")
            importer.import(m, receivedFiles.toMap())
            _state.value = ReceiveState.Done
            // Не вызываем stopAll сразу, даем время отправителю получить подтверждение.
            // Если рекламу успели перезапустить (start() отменяет job и проверка state
            // не пройдёт) — очистка не должна убить новую рекламу.
            pendingStopJob = scope.launch {
                kotlinx.coroutines.delay(2000)
                if (_state.value is ReceiveState.Done) nearby.stopAll()
            }
        } catch (e: CancellationException) {
            // Отмена scope — не ошибка импорта: показывать её на экране нельзя.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "P2P Receiver: Import failed")
            _state.value = ReceiveState.Error(e.message ?: "Ошибка импорта")
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
        nearby.stopAll()
        _state.value = ReceiveState.Idle
    }

    private companion object {
        const val DEFAULT_PEER_NAME = "Устройство"
    }
}
