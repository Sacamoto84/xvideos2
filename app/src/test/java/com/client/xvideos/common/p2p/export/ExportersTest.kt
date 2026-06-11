package com.client.xvideos.common.p2p.export

import com.client.xvideos.common.p2p.P2pType
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
}
