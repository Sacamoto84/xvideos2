package com.client.xvideos.common.p2p.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BundleLocatorsTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `X locator returns flat files and info as metadata`() {
        val root = tmp.newFolder("xdl")
        File(root, "42.mp4").writeText("v")
        File(root, "42.jpg").writeText("p")
        File(root, "42.info").writeText("{}")

        val located = XBundleLocator.locate(root, 42L)!!
        assertEquals("42.info", located.metadataFile.name)
        assertEquals(setOf("42.mp4", "42.jpg", "42.info"), located.files.map { it.name }.toSet())
        assertEquals(root, located.storeRoot)
    }

    @Test
    fun `X locator returns null when video missing`() {
        val root = tmp.newFolder("xdl")
        File(root, "42.info").writeText("{}")
        assertNull(XBundleLocator.locate(root, 42L))
    }

    @Test
    fun `R locator nests under userName`() {
        val root = tmp.newFolder("rdl")
        val dir = File(root, "lili").apply { mkdirs() }
        File(dir, "9.mp4").writeText("v")
        File(dir, "9.info").writeText("{}")

        val located = RBundleLocator.locate(root, "lili", "9")!!
        assertEquals("9.info", located.metadataFile.name)
        assertEquals(setOf("9.mp4", "9.info"), located.files.map { it.name }.toSet())
        assertEquals(root, located.storeRoot)
    }

    @Test
    fun `L locator lists folder files and metadata json`() {
        val root = tmp.newFolder("ldl")
        val folder = File(root, "album_z").apply { mkdirs() }
        File(folder, "media.jpg").writeText("m")
        File(folder, "preview.640x480.jpg").writeText("pp")
        File(folder, "metadata.json").writeText("{}")

        val located = LBundleLocator.locate(folder)!!
        assertEquals("metadata.json", located.metadataFile.name)
        assertEquals(root, located.storeRoot)
        assertEquals(3, located.files.size)
    }

    @Test
    fun `L locator returns null without metadata`() {
        val root = tmp.newFolder("ldl")
        val folder = File(root, "album_z").apply { mkdirs() }
        File(folder, "media.jpg").writeText("m")
        assertNull(LBundleLocator.locate(folder))
    }
}
