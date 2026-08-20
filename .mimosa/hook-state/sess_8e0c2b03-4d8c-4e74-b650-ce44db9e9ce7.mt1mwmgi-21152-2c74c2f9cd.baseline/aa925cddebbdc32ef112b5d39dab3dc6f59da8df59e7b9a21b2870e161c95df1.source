package com.client.xvideos.common.p2p

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

/** Найденный рядом телефон. */
data class P2pEndpoint(val id: String, val name: String)

/** Состояние экрана отправки. */
sealed interface ShareState {
    data object Idle : ShareState
    /** Подготовка файлов: скачивание item'а в outbox перед поиском устройств. */
    data object Preparing : ShareState
    data class Searching(val endpoints: List<P2pEndpoint>) : ShareState
    data object Connecting : ShareState
    data class Sending(val transferred: Long, val total: Long) : ShareState
    data object Done : ShareState
    data class Error(val message: String) : ShareState
}

/**
 * Отправляющая сторона: готовит бандл ([bundleProvider] — мгновенно для store
 * или скачивание в outbox), ищет телефоны, по подключению шлёт файлы, затем манифест.
 */
class P2pShareController(
    private val nearby: NearbyClient,
    private val scope: CoroutineScope,
    private val myName: String,
    private val bundleProvider: suspend () -> P2pExportBundle,
) {
    /** Готовый бандл (store): без фазы скачивания. */
    constructor(
        nearby: NearbyClient,
        scope: CoroutineScope,
        myName: String,
        bundle: P2pExportBundle,
    ) : this(nearby, scope, myName, bundleProvider = { bundle })

    private val _state = MutableStateFlow<ShareState>(ShareState.Idle)
    val state: StateFlow<ShareState> = _state.asStateFlow()

    private val endpoints = linkedMapOf<String, P2pEndpoint>()
    private var targetEndpoint: String? = null
    private var eventsJob: kotlinx.coroutines.Job? = null
    private var prepareJob: kotlinx.coroutines.Job? = null

    /** Бандл, закешированный после первой подготовки — рестарт не качает заново. */
    private var bundle: P2pExportBundle? = null

    /** Payload'ы, поставленные в очередь и ещё не подтверждённые доставкой. */
    private val pendingPayloads = mutableSetOf<Long>()
    private var allEnqueued = false

    fun start() {
        Timber.d("P2P Sender: Starting (prepare + discovery)...")
        endpoints.clear()
        targetEndpoint = null
        pendingPayloads.clear()
        allEnqueued = false
        eventsJob?.cancel()
        prepareJob?.cancel()
        _state.value = ShareState.Preparing
        prepareJob = scope.launch {
            val prepared = try {
                bundle ?: bundleProvider().also { bundle = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "P2P Sender: prepare failed")
                _state.value = ShareState.Error("Не удалось скачать файлы")
                return@launch
            }
            Timber.d("P2P Sender: bundle ready (${prepared.files.size} файлов), starting discovery")
            _state.value = ShareState.Searching(emptyList())
            eventsJob = scope.launch { nearby.events.collect { handle(it) } }
            nearby.startDiscovery()
        }
    }

    fun connectTo(endpointId: String) {
        if (_state.value is ShareState.Searching) {
            targetEndpoint = endpointId
            _state.value = ShareState.Connecting
            nearby.requestConnection(endpointId, myName)
        }
    }

    private fun handle(event: P2pEvent) {
        Timber.d("P2P Sender: handle event $event")
        when (event) {
            is P2pEvent.EndpointFound -> {
                Timber.d("P2P Sender: Endpoint found: ${event.name}")
                endpoints[event.endpointId] = P2pEndpoint(event.endpointId, event.name)
                if (_state.value is ShareState.Searching) _state.value = ShareState.Searching(endpoints.values.toList())
            }
            is P2pEvent.EndpointLost -> {
                Timber.d("P2P Sender: Endpoint lost: ${event.endpointId}")
                endpoints.remove(event.endpointId)
                if (_state.value is ShareState.Searching) _state.value = ShareState.Searching(endpoints.values.toList())
            }
            is P2pEvent.ConnectionInitiated -> {
                Timber.d("P2P Sender: Connection initiated. Automatically accepting for endpoint: ${event.endpointId}")
                _state.value = ShareState.Connecting
                nearby.acceptConnection(event.endpointId)
            }
            is P2pEvent.Connected -> {
                Timber.d("P2P Sender: Connected to ${event.endpointId}, starting transfer")
                nearby.stopDiscovery() // Останавливаем поиск после подключения для стабильности
                sendBundle(event.endpointId)
            }
            is P2pEvent.TransferProgress ->
                // GMS шлёт статусы payload'ов и после отправки манифеста — не затираем Done.
                if (_state.value is ShareState.Sending) {
                    _state.value = ShareState.Sending(event.transferred, event.total)
                }
            is P2pEvent.PayloadTransferred -> {
                // Done только когда ВСЁ реально доставлено: sendFile/sendBytes лишь ставят
                // в очередь, и на медленном канале (Bluetooth без upgrade) закрытие экрана
                // после раннего Done обрывало передачу у получателя.
                pendingPayloads.remove(event.payloadId)
                maybeDone()
            }
            is P2pEvent.PayloadTransferFailed -> {
                Timber.w("P2P Sender: payload ${event.payloadId} не доставлен")
                if (_state.value !is ShareState.Done) {
                    _state.value = ShareState.Error("Передача прервана")
                }
            }
            is P2pEvent.ConnectionRejected -> _state.value = ShareState.Error("Получатель отклонил")
            is P2pEvent.Disconnected -> {
                Timber.d("P2P Sender: Disconnected from ${event.endpointId}")
                if (_state.value !is ShareState.Done) {
                    _state.value = ShareState.Error("Соединение разорвано")
                }
                // После разрыва возвращаемся в поиск, чтобы можно было отправить снова
                scope.launch {
                    kotlinx.coroutines.delay(1000)
                    if (_state.value is ShareState.Error || _state.value is ShareState.Done) {
                        start()
                    }
                }
            }
            is P2pEvent.Failed -> _state.value = ShareState.Error(event.message)
            else -> Unit
        }
    }

    private fun sendBundle(endpointId: String) {
        val b = bundle ?: run {
            _state.value = ShareState.Error("Файлы не готовы")
            return
        }
        _state.value = ShareState.Sending(0, 0)
        pendingPayloads.clear()
        allEnqueued = false
        scope.launch {
            try {
                val payloadIds = HashMap<File, Long>()
                for (file in b.files) {
                    val id = nearby.sendFile(endpointId, file)
                    payloadIds[file] = id
                    pendingPayloads.add(id)
                }
                val manifest = P2pManifestFactory.create(
                    type = b.type,
                    storeRoot = b.storeRoot,
                    files = b.files,
                    metadataFile = b.metadataFile,
                    payloadIds = payloadIds,
                )
                pendingPayloads.add(nearby.sendBytes(endpointId, P2pManifestCodec.toBytes(manifest)))
                allEnqueued = true
                Timber.d("P2P Sender: всё в очереди, ждём подтверждений доставки (${pendingPayloads.size})")
                // Подтверждения могли прийти раньше, чем мы дошли сюда.
                maybeDone()
            } catch (e: CancellationException) {
                // Отмена отправки — не ошибка передачи.
                throw e
            } catch (e: Exception) {
                Timber.e(e, "P2P Sender: Transfer failed")
                _state.value = ShareState.Error(e.message ?: "Ошибка отправки")
            }
        }
    }

    private fun maybeDone() {
        if (allEnqueued && pendingPayloads.isEmpty() && _state.value is ShareState.Sending) {
            Timber.d("P2P Sender: все payload'ы доставлены")
            _state.value = ShareState.Done
        }
    }

    fun stop() {
        prepareJob?.cancel()
        nearby.stopAll()
    }
}
