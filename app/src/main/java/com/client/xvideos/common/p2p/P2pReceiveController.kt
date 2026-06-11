package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.imports.BundleImporter
import com.client.xvideos.common.p2p.nearby.NearbyClient
import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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

    private val receivedFiles = mutableMapOf<Long, File>()
    private var manifest: P2pManifest? = null
    private var currentEndpoint: String? = null

    fun start() {
        _state.value = ReceiveState.Advertising
        scope.launch { nearby.events.collect { handle(it) } }
        nearby.startAdvertising(deviceName)
    }

    private suspend fun handle(event: P2pEvent) {
        when (event) {
            is P2pEvent.ConnectionInitiated -> {
                currentEndpoint = event.endpointId
                _state.value = ReceiveState.Connecting(event.endpointName, event.authDigits)
            }
            is P2pEvent.Connected -> _state.value = ReceiveState.Receiving(0, 0)
            is P2pEvent.TransferProgress -> _state.value = ReceiveState.Receiving(event.transferred, event.total)
            is P2pEvent.FilePayloadReceived -> {
                receivedFiles[event.payloadId] = event.file
                tryImport()
            }
            is P2pEvent.BytesPayloadReceived -> {
                manifest = runCatching { P2pManifestCodec.fromBytes(event.bytes) }.getOrNull()
                if (manifest == null) _state.value = ReceiveState.Error("Битый манифест") else tryImport()
            }
            is P2pEvent.Disconnected ->
                if (_state.value !is ReceiveState.Done) _state.value = ReceiveState.Error("Соединение разорвано")
            is P2pEvent.Failed -> _state.value = ReceiveState.Error(event.message)
            else -> Unit
        }
    }

    private suspend fun tryImport() {
        val m = manifest ?: return
        if (!m.files.all { receivedFiles.containsKey(it.payloadId) }) return
        try {
            importer.import(m, receivedFiles.toMap())
            _state.value = ReceiveState.Done
            nearby.stopAll()
        } catch (e: Exception) {
            _state.value = ReceiveState.Error(e.message ?: "Ошибка импорта")
        }
    }

    fun confirmConnection() { currentEndpoint?.let { nearby.acceptConnection(it) } }
    fun reject() {
        currentEndpoint?.let { nearby.rejectConnection(it) }
        nearby.stopAll()
        _state.value = ReceiveState.Idle
    }
    fun stop() { nearby.stopAll() }
}
