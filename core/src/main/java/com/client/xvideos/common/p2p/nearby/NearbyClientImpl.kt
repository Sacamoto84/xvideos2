package com.client.xvideos.common.p2p.nearby

import android.content.Context
import android.os.ParcelFileDescriptor
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Реализация [NearbyClient] поверх Google Nearby Connections.
 * Стратегия P2P_CLUSTER (M:N, позволяет одновременно рекламироваться и искать).
 * ВАЖНО: advertiser и discoverer обязаны использовать одинаковую стратегию —
 * телефон со старой сборкой (P2P_STAR) не увидит телефон с новой.
 */
class NearbyClientImpl(context: Context) : NearbyClient {

    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val serviceId = "${appContext.packageName}.p2p"

    override val events = MutableSharedFlow<P2pEvent>(extraBufferCapacity = 64)

    /** Накопленные FILE-payload'ы по id, чтобы по завершению отдать готовый File. */
    private val incomingFiles = HashMap<Long, Payload>()

    private fun emit(event: P2pEvent) { events.tryEmit(event) }

    /**
     * Кладёт принятый FILE-payload во внутренний кеш и отдаёт обычный [File].
     *
     * Nearby сам решает, куда писать входящий файл, и это внешняя память
     * (`Android/data/<пакет>/files/Nearby`). Для приложения, которое всё держит
     * во `filesDir`, это чужая территория: на устройстве с недоступной или
     * сбойной SD-картой запись падает, а `asJavaFile()` начиная с Android 11
     * штатно возвращает null — прежний код в этом случае молча ронял payload,
     * поэтому приём заканчивался ничем и без ошибки на экране.
     *
     * Читаем через дескриптор — он работает независимо от того, куда Nearby
     * положил файл, — и копируем к себе. Дальше по тракту идёт уже наш файл.
     */
    private fun receiveToCache(payloadId: Long, file: Payload.File): File? = runCatching {
        val target = File(nearbyCacheDir, payloadId.toString())
        val descriptor = file.asParcelFileDescriptor()
        if (descriptor != null) {
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            Timber.i("P2P: payload $payloadId принят -> $target (${target.length()} байт)")
            return@runCatching target
        }

        // Fallback для старых Android, где дескриптора нет, а java.io.File есть.
        val javaFile = file.asJavaFile()
        if (javaFile != null) {
            javaFile.copyTo(target, overwrite = true)
            javaFile.delete()
            Timber.i("P2P: payload $payloadId принят через javaFile -> $target")
            return@runCatching target
        }

        Timber.e("P2P: payload $payloadId — ни дескриптора, ни файла")
        null
    }.onFailure {
        Timber.e(it, "P2P: не удалось забрать payload $payloadId из Nearby")
    }.getOrNull()

    /** Куда складываем принятое из Nearby: внутренний кеш, чистится при старте. */
    private val nearbyCacheDir: File by lazy {
        File(appContext.cacheDir, "p2p-nearby").apply { mkdirs() }
    }

    override fun startAdvertising(name: String) {
        Timber.d("P2P: startAdvertising(name=$name, serviceId=$serviceId)")
        client.stopAdvertising()
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        client.startAdvertising(name, serviceId, connectionLifecycle, options)
            .addOnSuccessListener { Timber.d("P2P: startAdvertising SUCCESS") }
            .addOnFailureListener { e ->
                Timber.e(e, "P2P: startAdvertising FAILURE")
                if (e is ApiException && e.statusCode == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING) return@addOnFailureListener
                emit(P2pEvent.Failed("Advertising: ${e.message}"))
            }
    }

    override fun startDiscovery() {
        Timber.d("P2P: startDiscovery(serviceId=$serviceId)")
        client.stopDiscovery()
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        client.startDiscovery(serviceId, discoveryCallback, options)
            .addOnSuccessListener { Timber.d("P2P: startDiscovery SUCCESS") }
            .addOnFailureListener { e ->
                Timber.e(e, "P2P: startDiscovery FAILURE")
                if (e is ApiException && e.statusCode == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING) return@addOnFailureListener
                emit(P2pEvent.Failed("Discovery: ${e.message}"))
            }
    }

    override fun requestConnection(endpointId: String, myName: String) {
        Timber.d("P2P: requestConnection(endpointId=$endpointId, myName=$myName)")
        client.requestConnection(myName, endpointId, connectionLifecycle)
            .addOnSuccessListener { 
                Timber.d("P2P: requestConnection request SUCCESS (awaiting onConnectionInitiated)") 
            }
            .addOnFailureListener { e ->
                Timber.e(e, "P2P: requestConnection FAILURE")
                if (e is ApiException && e.statusCode == ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL) return@addOnFailureListener
                emit(P2pEvent.Failed("Connect: ${e.message}"))
            }
    }

    override fun acceptConnection(endpointId: String) {
        Timber.d("P2P: acceptConnection(endpointId=$endpointId)")
        client.acceptConnection(endpointId, payloadCallback)
            .addOnSuccessListener { Timber.d("P2P: acceptConnection SUCCESS (waiting for other side)") }
            .addOnFailureListener { e ->
                Timber.e(e, "P2P: acceptConnection FAILURE")
                if (e is ApiException && e.statusCode == ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL) return@addOnFailureListener
                emit(P2pEvent.Failed("Accept: ${e.message}"))
            }
    }

    override fun rejectConnection(endpointId: String) {
        client.rejectConnection(endpointId)
            .addOnFailureListener { e ->
                if (e is ApiException && e.statusCode == ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL) return@addOnFailureListener
            }
    }

    override suspend fun sendFile(endpointId: String, file: java.io.File): Long =
        suspendCancellableCoroutine { cont ->
            try {
                val payload = Payload.fromFile(file)
                client.sendPayload(endpointId, payload)
                    .addOnSuccessListener { cont.resume(payload.id) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }

    override fun sendBytes(endpointId: String, bytes: ByteArray): Long {
        val payload = Payload.fromBytes(bytes)
        client.sendPayload(endpointId, payload)
        return payload.id
    }

    override fun stopDiscovery() {
        client.stopDiscovery()
    }

    override fun stopAdvertising() {
        client.stopAdvertising()
    }

    override fun stopAll() {
        runCatching { client.stopAllEndpoints() }
        runCatching { client.stopAdvertising() }
        runCatching { client.stopDiscovery() }
        incomingFiles.clear()
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Timber.d("P2P: onEndpointFound(id=$endpointId, name=${info.endpointName})")
            emit(P2pEvent.EndpointFound(endpointId, info.endpointName))
        }
        override fun onEndpointLost(endpointId: String) {
            Timber.d("P2P: onEndpointLost(id=$endpointId)")
            emit(P2pEvent.EndpointLost(endpointId))
        }
    }

    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Timber.d("P2P: onConnectionInitiated(id=$endpointId, name=${info.endpointName}, auth=${info.authenticationDigits})")
            emit(P2pEvent.ConnectionInitiated(endpointId, info.endpointName))
        }
        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            val status = resolution.status
            Timber.d("P2P: onConnectionResult(id=$endpointId, status=${status.statusCode}, msg=${status.statusMessage})")
            when (status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> emit(P2pEvent.Connected(endpointId))
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> emit(P2pEvent.ConnectionRejected(endpointId))
                ConnectionsStatusCodes.STATUS_ERROR -> emit(P2pEvent.Failed("Ошибка соединения (Status Error)"))
                else -> emit(P2pEvent.Failed("Результат: ${status.statusCode} ${status.statusMessage ?: ""}"))
            }
        }
        override fun onDisconnected(endpointId: String) {
            Timber.d("P2P: onDisconnected(id=$endpointId)")
            emit(P2pEvent.Disconnected(endpointId))
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> payload.asBytes()?.let { emit(P2pEvent.BytesPayloadReceived(it)) }
                Payload.Type.FILE -> incomingFiles[payload.id] = payload
                else -> Unit
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            emit(P2pEvent.TransferProgress(update.payloadId, update.bytesTransferred, update.totalBytes))
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    // Отправителю — подтверждение доставки (Done только после всех).
                    emit(P2pEvent.PayloadTransferred(update.payloadId))
                    val payload = incomingFiles.remove(update.payloadId) ?: return
                    val received = payload.asFile()?.let { receiveToCache(update.payloadId, it) }
                    if (received != null) {
                        emit(P2pEvent.FilePayloadReceived(update.payloadId, received))
                    } else {
                        Timber.e("P2P: FILE payload ${update.payloadId} прочитать не удалось")
                        emit(P2pEvent.PayloadTransferFailed(update.payloadId))
                    }
                }
                PayloadTransferUpdate.Status.FAILURE,
                PayloadTransferUpdate.Status.CANCELED -> {
                    Timber.w("P2P: payload ${update.payloadId} не доставлен (status=${update.status})")
                    emit(P2pEvent.PayloadTransferFailed(update.payloadId))
                }
                else -> Unit
            }
        }
    }

    private companion object {
        val STRATEGY: Strategy = Strategy.P2P_CLUSTER
    }
}
