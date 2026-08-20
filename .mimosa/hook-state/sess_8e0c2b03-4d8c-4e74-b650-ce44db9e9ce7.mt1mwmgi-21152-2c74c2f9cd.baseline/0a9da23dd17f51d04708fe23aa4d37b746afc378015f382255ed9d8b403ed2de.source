package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.nearby.NearbyClient
import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

/** Фейковый транспорт: ручная эмиссия событий + запись вызовов. */
class FakeNearbyClient : NearbyClient {
    override val events = MutableSharedFlow<P2pEvent>(extraBufferCapacity = 64)

    val sentFiles = mutableListOf<Pair<String, File>>()
    val sentBytes = mutableListOf<ByteArray>()
    val accepted = mutableListOf<String>()
    var advertising = false
    var discovering = false
    var stopped = false
    var nextPayloadId = 1000L

    suspend fun emit(event: P2pEvent) { events.emit(event) }

    override fun startAdvertising(name: String) { advertising = true }
    override fun startDiscovery() { discovering = true }
    override fun requestConnection(endpointId: String, myName: String) {}
    override fun acceptConnection(endpointId: String) { accepted += endpointId }
    override fun rejectConnection(endpointId: String) {}
    override suspend fun sendFile(endpointId: String, file: File): Long {
        val id = nextPayloadId++
        sentFiles += endpointId to file
        return id
    }
    override fun sendBytes(endpointId: String, bytes: ByteArray): Long {
        sentBytes += bytes
        return nextPayloadId++
    }
    override fun stopDiscovery() { discovering = false }
    override fun stopAdvertising() { advertising = false }
    override fun stopAll() {
        stopped = true
        advertising = false
        discovering = false
    }
}
