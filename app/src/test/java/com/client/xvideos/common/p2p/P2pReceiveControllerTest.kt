package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.imports.BundleImporter
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
class P2pReceiveControllerTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `assembles bundle when manifest arrives before files and imports once complete`() =
        runTest(UnconfinedTestDispatcher()) {
            val fake = FakeNearbyClient()
            var imported: Pair<P2pManifest, Map<Long, File>>? = null
            val importer = BundleImporter { manifest, files -> imported = manifest to files }

            val controller = P2pReceiveController(
                nearby = fake,
                importer = importer,
                scope = backgroundScope,
                deviceName = "Pixel-Test",
            )
            controller.start()

            val fileA = tmp.newFile("a").apply { writeText("A") }
            val manifest = P2pManifest(
                type = P2pType.X,
                metadataFileName = "1.info",
                files = listOf(P2pManifestFile("1.mp4", "1.mp4", 5L, 1L)),
            )

            // Манифест приходит раньше файла.
            fake.emit(P2pEvent.ConnectionInitiated("E1", "Other", "1234"))
            fake.emit(P2pEvent.Connected("E1"))
            fake.emit(P2pEvent.BytesPayloadReceived(P2pManifestCodec.toBytes(manifest)))
            assertTrue("Импорт не должен случиться до прихода файла", imported == null)

            fake.emit(P2pEvent.FilePayloadReceived(5L, fileA))

            assertEquals(manifest, imported!!.first)
            assertEquals(fileA, imported!!.second.getValue(5L))
            assertEquals(ReceiveState.Done, controller.state.value)
            assertTrue(fake.stopped)
        }

    @Test
    fun `confirmConnection accepts the current endpoint`() = runTest(UnconfinedTestDispatcher()) {
        val fake = FakeNearbyClient()
        val controller = P2pReceiveController(fake, { _, _ -> }, backgroundScope, "Pixel-Test")
        controller.start()

        fake.emit(P2pEvent.ConnectionInitiated("E9", "Other", "0000"))
        controller.confirmConnection()

        assertEquals(listOf("E9"), fake.accepted)
    }
}
