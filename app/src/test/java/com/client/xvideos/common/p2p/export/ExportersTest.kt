package com.client.xvideos.common.p2p.export

import com.client.xvideos.common.p2p.P2pManifestFactory
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.p2p.mirrorRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExportersTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `X exporter builds bundle from store root and id`() {
        val root = tmp.newFolder("xdl")
        File(root, "7.mp4").writeText("v")
        File(root, "7.info").writeText("{}")

        val bundle = XExporter.export(root, id = 7L)!!
        assertEquals(P2pType.X, bundle.type)
        assertEquals(root, bundle.storeRoot)
        assertEquals("7.info", bundle.metadataFile!!.name)
    }

    @Test
    fun `X exporter returns null when not downloaded`() {
        val root = tmp.newFolder("xdl")
        assertNull(XExporter.export(root, id = 7L))
    }

    @Test
    fun `L exporter builds bundle from item folder`() {
        val root = tmp.newFolder("ldl")
        val folder = File(root, "album_q").apply { mkdirs() }
        File(folder, "media.jpg").writeText("m")
        File(folder, "metadata.json").writeText("{}")

        val bundle = LExporter.export(folder)!!
        assertEquals(P2pType.L, bundle.type)
        assertEquals(root, bundle.storeRoot)
    }

    @Test
    fun `L exporter from outbox mirror yields same relative paths as store`() {
        val main = tmp.newFolder("xvideos")
        val storeLikes = File(main, "L/Likes").apply { mkdirs() }
        val outboxLikes = mirrorRoot(File(main, "outbox"), main, storeLikes).apply { mkdirs() }
        for (root in listOf(storeLikes, outboxLikes)) {
            val folder = File(root, "album_q").apply { mkdirs() }
            File(folder, "media.jpg").writeText("m")
            File(folder, "metadata.json").writeText("{}")
        }

        fun relPaths(root: File): List<String> {
            val b = LExporter.export(File(root, "album_q"))!!
            val manifest = P2pManifestFactory.create(
                type = b.type,
                storeRoot = b.storeRoot,
                files = b.files,
                metadataFile = b.metadataFile,
                payloadIds = b.files.withIndex().associate { (i, f) -> f to (1000L + i) },
            )
            return manifest.files.map { it.relativePath }
        }

        assertEquals(relPaths(storeLikes), relPaths(outboxLikes))
    }
}
