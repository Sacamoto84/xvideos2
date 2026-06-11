package com.client.xvideos.common.p2p.nearby

import kotlinx.coroutines.flow.Flow
import java.io.File

/** События транспорта Nearby, общие для отправителя и получателя. */
sealed interface P2pEvent {
    data class EndpointFound(val endpointId: String, val name: String) : P2pEvent
    data class EndpointLost(val endpointId: String) : P2pEvent
    data class ConnectionInitiated(val endpointId: String, val endpointName: String, val authDigits: String) : P2pEvent
    data class Connected(val endpointId: String) : P2pEvent
    data class ConnectionRejected(val endpointId: String) : P2pEvent
    data class Disconnected(val endpointId: String) : P2pEvent
    data class FilePayloadReceived(val payloadId: Long, val file: File) : P2pEvent
    data class BytesPayloadReceived(val bytes: ByteArray) : P2pEvent {
        override fun equals(other: Any?) = this === other || (other is BytesPayloadReceived && bytes.contentEquals(other.bytes))
        override fun hashCode() = bytes.contentHashCode()
    }
    data class TransferProgress(val payloadId: Long, val transferred: Long, val total: Long) : P2pEvent
    data class Failed(val message: String) : P2pEvent
}

/** Тонкая обёртка над Nearby Connections. Реализация — [NearbyClientImpl]; в тестах — фейк. */
interface NearbyClient {
    val events: Flow<P2pEvent>

    fun startAdvertising(name: String)
    fun startDiscovery()
    fun requestConnection(endpointId: String, myName: String)
    fun acceptConnection(endpointId: String)
    fun rejectConnection(endpointId: String)

    /** Отправить файл; возвращает payloadId (одинаков на обоих телефонах). */
    suspend fun sendFile(endpointId: String, file: File): Long

    /** Отправить control-сообщение (манифест). */
    fun sendBytes(endpointId: String, bytes: ByteArray)

    fun stopAll()
}
