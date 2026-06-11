package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.nearby.NearbyClient
import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Найденный рядом телефон. */
data class P2pEndpoint(val id: String, val name: String)

/** Состояние экрана отправки. */
sealed interface ShareState {
    data object Idle : ShareState
    data class Searching(val endpoints: List<P2pEndpoint>) : ShareState
    data class Connecting(val authDigits: String?) : ShareState
    data class Sending(val transferred: Long, val total: Long) : ShareState
    data object Done : ShareState
    data class Error(val message: String) : ShareState
}

/**
 * Отправляющая сторона: ищет телефоны, по подключению шлёт файлы [bundle], затем манифест.
 */
class P2pShareController(
    private val nearby: NearbyClient,
    private val scope: CoroutineScope,
    private val myName: String,
    private val bundle: P2pExportBundle,
) {
    private val _state = MutableStateFlow<ShareState>(ShareState.Idle)
    val state: StateFlow<ShareState> = _state.asStateFlow()

    private val endpoints = linkedMapOf<String, P2pEndpoint>()
    private var targetEndpoint: String? = null

    fun start() {
        _state.value = ShareState.Searching(emptyList())
        scope.launch { nearby.events.collect { handle(it) } }
        nearby.startDiscovery()
    }

    fun connectTo(endpointId: String) {
        targetEndpoint = endpointId
        _state.value = ShareState.Connecting(null)
        nearby.requestConnection(endpointId, myName)
    }

    private fun handle(event: P2pEvent) {
        when (event) {
            is P2pEvent.EndpointFound -> {
                endpoints[event.endpointId] = P2pEndpoint(event.endpointId, event.name)
                if (_state.value is ShareState.Searching) _state.value = ShareState.Searching(endpoints.values.toList())
            }
            is P2pEvent.EndpointLost -> {
                endpoints.remove(event.endpointId)
                if (_state.value is ShareState.Searching) _state.value = ShareState.Searching(endpoints.values.toList())
            }
            is P2pEvent.ConnectionInitiated -> _state.value = ShareState.Connecting(event.authDigits)
            is P2pEvent.Connected -> sendBundle(event.endpointId)
            is P2pEvent.TransferProgress -> _state.value = ShareState.Sending(event.transferred, event.total)
            is P2pEvent.ConnectionRejected -> _state.value = ShareState.Error("Получатель отклонил")
            is P2pEvent.Disconnected ->
                if (_state.value !is ShareState.Done) _state.value = ShareState.Error("Соединение разорвано")
            is P2pEvent.Failed -> _state.value = ShareState.Error(event.message)
            else -> Unit
        }
    }

    private fun sendBundle(endpointId: String) {
        _state.value = ShareState.Sending(0, 0)
        scope.launch {
            try {
                val payloadIds = HashMap<File, Long>()
                for (file in bundle.files) payloadIds[file] = nearby.sendFile(endpointId, file)
                val manifest = P2pManifestFactory.create(
                    type = bundle.type,
                    storeRoot = bundle.storeRoot,
                    files = bundle.files,
                    metadataFile = bundle.metadataFile,
                    payloadIds = payloadIds,
                )
                nearby.sendBytes(endpointId, P2pManifestCodec.toBytes(manifest))
            } catch (e: Exception) {
                _state.value = ShareState.Error(e.message ?: "Ошибка отправки")
            }
        }
    }

    fun stop() { nearby.stopAll() }
}
