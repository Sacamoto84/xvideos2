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
    fun `imports into store root for type and triggers refresh`() = runTest {
        val xRoot = tmp.newFolder("xRoot")
        val received = tmp.newFile("p1").apply { writeText("VID") }
        var refreshed: P2pType? = null

        val importer = StoreBundleImporter(
            storeRootFor = { type -> if (type == P2pType.X) xRoot else error("unexpected") },
            refreshFor = { type -> refreshed = type },
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
        assertEquals(P2pType.X, refreshed)
    }
}
