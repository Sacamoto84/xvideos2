package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class P2pShareControllerTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `on connected sends every file then a manifest describing them`() =
        runTest(UnconfinedTestDispatcher()) {
            val root = tmp.newFolder("xdl")
            val mp4 = File(root, "3.mp4").apply { writeText("VVV") }
            val info = File(root, "3.info").apply { writeText("{}") }
            val bundle = P2pExportBundle(P2pType.X, root, listOf(mp4, info), info)

            val fake = FakeNearbyClient()
            val controller = P2pShareController(fake, backgroundScope, myName = "Sender", bundle = bundle)
            controller.start()

            fake.emit(P2pEvent.EndpointFound("E1", "Receiver"))
            controller.connectTo("E1")
            fake.emit(P2pEvent.Connected("E1"))

            assertEquals(2, fake.sentFiles.size)
            assertEquals(1, fake.sentBytes.size)
            val manifest = P2pManifestCodec.fromBytes(fake.sentBytes.first())
            assertEquals(P2pType.X, manifest.type)
            assertEquals(setOf("3.mp4", "3.info"), manifest.files.map { it.name }.toSet())
            assertTrue(manifest.files.all { it.payloadId >= 1000L })
        }

    @Test
    fun `done only after all payloads are delivered`() =
        runTest(UnconfinedTestDispatcher()) {
            val root = tmp.newFolder("xdl")
            val mp4 = File(root, "3.mp4").apply { writeText("VVV") }
            val info = File(root, "3.info").apply { writeText("{}") }
            val bundle = P2pExportBundle(P2pType.X, root, listOf(mp4, info), info)

            val fake = FakeNearbyClient()
            val controller = P2pShareController(fake, backgroundScope, myName = "Sender", bundle = bundle)
            controller.start()

            fake.emit(P2pEvent.EndpointFound("E1", "Receiver"))
            controller.connectTo("E1")
            fake.emit(P2pEvent.Connected("E1"))

            // Всё поставлено в очередь (файлы 1000,1001 + манифест 1002),
            // но НИЧЕГО ещё не доставлено — Done рано, медленный канал оборвётся.
            assertTrue(controller.state.value is ShareState.Sending)

            fake.emit(P2pEvent.PayloadTransferred(1000L))
            fake.emit(P2pEvent.PayloadTransferred(1001L))
            assertTrue(controller.state.value is ShareState.Sending)

            fake.emit(P2pEvent.PayloadTransferred(1002L))
            assertEquals(ShareState.Done, controller.state.value)
        }

    @Test
    fun `payload transfer failure puts sender into error`() =
        runTest(UnconfinedTestDispatcher()) {
            val root = tmp.newFolder("xdl2")
            val mp4 = File(root, "4.mp4").apply { writeText("VVV") }
            val bundle = P2pExportBundle(P2pType.X, root, listOf(mp4), null)

            val fake = FakeNearbyClient()
            val controller = P2pShareController(fake, backgroundScope, myName = "Sender", bundle = bundle)
            controller.start()

            fake.emit(P2pEvent.EndpointFound("E1", "Receiver"))
            controller.connectTo("E1")
            fake.emit(P2pEvent.Connected("E1"))

            fake.emit(P2pEvent.PayloadTransferFailed(1000L))

            assertTrue(controller.state.value is ShareState.Error)
        }

    @Test
    fun `late transfer progress after done does not revert state to sending`() =
        runTest(UnconfinedTestDispatcher()) {
            val root = tmp.newFolder("xdl")
            val mp4 = File(root, "3.mp4").apply { writeText("VVV") }
            val bundle = P2pExportBundle(P2pType.X, root, listOf(mp4), null)

            val fake = FakeNearbyClient()
            val controller = P2pShareController(fake, backgroundScope, myName = "Sender", bundle = bundle)
            controller.start()

            fake.emit(P2pEvent.EndpointFound("E1", "Receiver"))
            controller.connectTo("E1")
            fake.emit(P2pEvent.Connected("E1"))
            // Доставлены файл (1000) и манифест (1001).
            fake.emit(P2pEvent.PayloadTransferred(1000L))
            fake.emit(P2pEvent.PayloadTransferred(1001L))
            assertEquals(ShareState.Done, controller.state.value)

            // GMS шлёт статусы payload'ов и после завершения — Done не должен откатиться.
            fake.emit(P2pEvent.TransferProgress(1000L, 3L, 3L))

            assertEquals(ShareState.Done, controller.state.value)
        }

    @Test
    fun `stays in preparing while provider downloads then searches`() = runTest {
        val gate = CompletableDeferred<P2pExportBundle>()
        val fake = FakeNearbyClient()
        val controller = P2pShareController(
            nearby = fake,
            scope = backgroundScope,
            myName = "Sender",
            bundleProvider = { gate.await() },
        )

        controller.start()
        runCurrent()
        assertEquals(ShareState.Preparing, controller.state.value)

        val root = tmp.newFolder("outbox")
        val mp4 = File(root, "3.mp4").apply { writeText("V") }
        gate.complete(P2pExportBundle(P2pType.X, root, listOf(mp4), null))
        runCurrent()
        assertTrue(controller.state.value is ShareState.Searching)
    }

    @Test
    fun `prepare failure puts controller into error`() =
        runTest(UnconfinedTestDispatcher()) {
            val fake = FakeNearbyClient()
            val controller = P2pShareController(
                nearby = fake,
                scope = backgroundScope,
                myName = "Sender",
                bundleProvider = { error("network down") },
            )

            controller.start()

            assertTrue(controller.state.value is ShareState.Error)
        }

    @Test
    fun `bundle is prepared once across restarts`() =
        runTest(UnconfinedTestDispatcher()) {
            val root = tmp.newFolder("outbox")
            val mp4 = File(root, "3.mp4").apply { writeText("V") }
            val bundle = P2pExportBundle(P2pType.X, root, listOf(mp4), null)
            var calls = 0
            val fake = FakeNearbyClient()
            val controller = P2pShareController(
                nearby = fake,
                scope = backgroundScope,
                myName = "Sender",
                bundleProvider = { calls++; bundle },
            )

            controller.start()
            controller.start() // рестарт после разрыва — скачивание не повторяется

            assertEquals(1, calls)
            assertTrue(controller.state.value is ShareState.Searching)
        }
}
