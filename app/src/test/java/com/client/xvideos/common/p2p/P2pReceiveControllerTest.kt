package com.client.xvideos.common.p2p

import com.client.xvideos.common.p2p.imports.BundleImporter
import com.client.xvideos.common.p2p.nearby.P2pEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            fake.emit(P2pEvent.ConnectionInitiated("E1", "Other"))
            fake.emit(P2pEvent.Connected("E1"))
            fake.emit(P2pEvent.BytesPayloadReceived(P2pManifestCodec.toBytes(manifest)))
            assertTrue("Импорт не должен случиться до прихода файла", imported == null)

            fake.emit(P2pEvent.FilePayloadReceived(5L, fileA))

            assertEquals(manifest, imported!!.first)
            assertEquals(fileA, imported!!.second.getValue(5L))
            assertEquals(ReceiveState.Done, controller.state.value)

            // Отложенная очистка срабатывает, если рекламу никто не перезапустил.
            advanceTimeBy(2_500)
            assertTrue(fake.stopped)
        }

    @Test
    fun `connection initiated is auto-accepted`() = runTest(UnconfinedTestDispatcher()) {
        val fake = FakeNearbyClient()
        val controller = P2pReceiveController(fake, { _, _ -> }, backgroundScope, "Pixel-Test")
        controller.start()

        fake.emit(P2pEvent.ConnectionInitiated("E9", "Other"))

        assertEquals(listOf("E9"), fake.accepted)
    }

    @Test
    fun `peer name captured from connection initiated`() = runTest(UnconfinedTestDispatcher()) {
        val fake = FakeNearbyClient()
        val controller = P2pReceiveController(fake, { _, _ -> }, backgroundScope, "Pixel-Test")
        controller.start()

        fake.emit(P2pEvent.ConnectionInitiated("E1", "Galaxy S24"))

        assertEquals("Galaxy S24", controller.peerName)
    }

    @Test
    fun `disconnect while advertising keeps advertising instead of error`() =
        runTest(UnconfinedTestDispatcher()) {
            val fake = FakeNearbyClient()
            val controller = P2pReceiveController(fake, { _, _ -> }, backgroundScope, "Pixel-Test")
            controller.start()

            // Запоздавший disconnect от предыдущей сессии не должен ронять приёмник в Error.
            fake.emit(P2pEvent.Disconnected("E-old"))

            assertEquals(ReceiveState.Advertising, controller.state.value)
            assertTrue(fake.advertising)
        }

    @Test
    fun `restart after done is not killed by delayed cleanup`() =
        runTest(UnconfinedTestDispatcher()) {
            val fake = FakeNearbyClient()
            val controller = P2pReceiveController(
                nearby = fake,
                importer = { _, _ -> },
                scope = backgroundScope,
                deviceName = "Pixel-Test",
            )
            controller.start()

            val fileA = tmp.newFile("b").apply { writeText("B") }
            val manifest = P2pManifest(
                type = P2pType.X,
                metadataFileName = "1.info",
                files = listOf(P2pManifestFile("1.mp4", "1.mp4", 7L, 1L)),
            )
            fake.emit(P2pEvent.ConnectionInitiated("E1", "Other"))
            fake.emit(P2pEvent.Connected("E1"))
            fake.emit(P2pEvent.BytesPayloadReceived(P2pManifestCodec.toBytes(manifest)))
            fake.emit(P2pEvent.FilePayloadReceived(7L, fileA))
            assertEquals(ReceiveState.Done, controller.state.value)

            // Менеджер сразу перезапускает рекламу после Done…
            controller.start()
            // …и отложенный stopAll не должен её убить.
            advanceTimeBy(2_500)

            assertTrue(fake.advertising)
            assertFalse(fake.stopped)
            assertEquals(ReceiveState.Advertising, controller.state.value)
        }
}
