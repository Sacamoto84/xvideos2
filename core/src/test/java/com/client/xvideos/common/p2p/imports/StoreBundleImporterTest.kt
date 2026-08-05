package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pManifestFile
import com.client.xvideos.common.p2p.P2pType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StoreBundleImporterTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `installs via inbox then merges into store root and triggers refresh`() = runTest {
        val main = tmp.newFolder("xvideos")
        val xRoot = File(main, "X/Download").apply { mkdirs() }
        val inbox = File(main, "inbox").apply { mkdirs() }
        val received = tmp.newFile("p1").apply { writeText("VID") }
        var refreshed: P2pType? = null

        val importer = StoreBundleImporter(
            storeRootFor = { type -> if (type == P2pType.X) xRoot else error("unexpected") },
            refreshFor = { type -> refreshed = type },
            inboxRoot = inbox,
            mainRoot = main,
        )

        val manifest = P2pManifest(
            type = P2pType.X,
            metadataFileName = "8.info",
            files = listOf(P2pManifestFile("8.mp4", "8.mp4", 1L, 3L)),
        )

        importer.import(manifest, mapOf(1L to received))

        val target = File(xRoot, "8.mp4")
        assertTrue(target.exists())
        assertEquals("VID", target.readText())
        // staging опустошён после merge
        assertTrue(inbox.listFiles().isNullOrEmpty())
        assertEquals(P2pType.X, refreshed)
    }

    @Test
    fun `L_ALBUM manifest lands in albums root and refreshes`() = runTest {
        val main = tmp.newFolder("xvideos")
        val albumsRoot = File(main, "L/Album").apply { mkdirs() }
        val inbox = File(main, "inbox").apply { mkdirs() }
        val received = tmp.newFile("p2").apply { writeText("{json}") }
        var refreshed: P2pType? = null

        val importer = StoreBundleImporter(
            storeRootFor = { type -> if (type == P2pType.L_ALBUM) albumsRoot else error("unexpected") },
            refreshFor = { refreshed = it },
            inboxRoot = inbox,
            mainRoot = main,
        )

        importer.import(
            P2pManifest(
                type = P2pType.L_ALBUM,
                metadataFileName = "42.album",
                files = listOf(P2pManifestFile("42.album", "42.album", 1L, 6L)),
            ),
            mapOf(1L to received),
        )

        assertEquals("{json}", File(albumsRoot, "42.album").readText())
        assertTrue(inbox.listFiles().isNullOrEmpty())
        assertEquals(P2pType.L_ALBUM, refreshed)
    }
}
