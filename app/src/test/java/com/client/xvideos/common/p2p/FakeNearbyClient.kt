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
    var stopped = false
    var nextPayloadId = 1000L

    suspend fun emit(event: P2pEvent) { events.emit(event) }

    override fun startAdvertising(name: String) {}
    override fun startDiscovery() {}
    override fun requestConnection(endpointId: String, myName: String) {}
    override fun acceptConnection(endpointId: String) { accepted += endpointId }
    override fun rejectConnection(endpointId: String) {}
    override suspend fun sendFile(endpointId: String, file: File): Long {
        val id = nextPayloadId++
        sentFiles += endpointId to file
        return id
    }
    override fun sendBytes(endpointId: String, bytes: ByteArray) { sentBytes += bytes }
    override fun stopAll() { stopped = true }
}
