package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
            assertEquals(ShareState.Done, controller.state.value)

            // GMS шлёт статусы payload'ов и после завершения — Done не должен откатиться.
            fake.emit(P2pEvent.TransferProgress(1000L, 3L, 3L))

            assertEquals(ShareState.Done, controller.state.value)
        }
}
