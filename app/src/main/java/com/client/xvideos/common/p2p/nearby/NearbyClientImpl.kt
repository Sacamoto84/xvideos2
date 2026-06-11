package com.client.xvideos.common.p2p.nearby

import android.content.Context
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Реализация [NearbyClient] поверх Google Nearby Connections.
 * Стратегия P2P_POINT_TO_POINT (1:1, максимальная скорость, BT + Wi-Fi).
 */
class NearbyClientImpl(context: Context) : NearbyClient {

    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val serviceId = "${appContext.packageName}.p2p"

    override val events = MutableSharedFlow<P2pEvent>(extraBufferCapacity = 64)

    /** Накопленные FILE-payload'ы по id, чтобы по завершению отдать готовый File. */
    private val incomingFiles = HashMap<Long, Payload>()

    private fun emit(event: P2pEvent) { events.tryEmit(event) }

    override fun startAdvertising(name: String) {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        client.startAdvertising(name, serviceId, connectionLifecycle, options)
            .addOnFailureListener { emit(P2pEvent.Failed("Advertising: ${it.message}")) }
    }

    override fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        client.startDiscovery(serviceId, discoveryCallback, options)
            .addOnFailureListener { emit(P2pEvent.Failed("Discovery: ${it.message}")) }
    }

    override fun requestConnection(endpointId: String, myName: String) {
        client.requestConnection(myName, endpointId, connectionLifecycle)
            .addOnFailureListener { emit(P2pEvent.Failed("Connect: ${it.message}")) }
    }

    override fun acceptConnection(endpointId: String) {
        client.acceptConnection(endpointId, payloadCallback)
            .addOnFailureListener { emit(P2pEvent.Failed("Accept: ${it.message}")) }
    }

    override fun rejectConnection(endpointId: String) {
        client.rejectConnection(endpointId)
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

    override fun sendBytes(endpointId: String, bytes: ByteArray) {
        client.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    override fun stopAll() {
        runCatching { client.stopAllEndpoints() }
        runCatching { client.stopAdvertising() }
        runCatching { client.stopDiscovery() }
        incomingFiles.clear()
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            emit(P2pEvent.EndpointFound(endpointId, info.endpointName))
        }
        override fun onEndpointLost(endpointId: String) {
            emit(P2pEvent.EndpointLost(endpointId))
        }
    }

    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            emit(P2pEvent.ConnectionInitiated(endpointId, info.endpointName, info.authenticationDigits))
        }
        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> emit(P2pEvent.Connected(endpointId))
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> emit(P2pEvent.ConnectionRejected(endpointId))
                else -> emit(P2pEvent.Failed("Result: ${resolution.status.statusCode}"))
            }
        }
        override fun onDisconnected(endpointId: String) {
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
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val payload = incomingFiles.remove(update.payloadId) ?: return
                val javaFile = payload.asFile()?.asJavaFile()
                if (javaFile != null) {
                    emit(P2pEvent.FilePayloadReceived(update.payloadId, javaFile))
                } else {
                    Timber.w("P2P: FILE payload ${update.payloadId} без javaFile")
                }
            }
        }
    }

    private companion object {
        val STRATEGY: Strategy = Strategy.P2P_POINT_TO_POINT
    }
}
