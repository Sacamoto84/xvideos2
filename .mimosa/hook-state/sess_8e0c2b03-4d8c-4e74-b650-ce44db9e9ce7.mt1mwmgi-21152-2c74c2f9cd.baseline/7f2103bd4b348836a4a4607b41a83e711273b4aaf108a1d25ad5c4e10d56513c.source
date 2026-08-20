package com.client.xvideos.r.common.downloader

import com.client.xvideos.common.p2p.P2pType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RMetaBundleTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `bundle with info only when no jpg present`() {
        val root = tmp.newFolder("export")

        val bundle = buildRMetaBundle(root, "creator", "abc", infoJson = "{\"id\":\"abc\"}")

        assertEquals(P2pType.R, bundle.type)
        assertEquals(root, bundle.storeRoot)
        assertEquals(1, bundle.files.size)
        val info = bundle.files.single()
        assertEquals(File(root, "creator/abc.info"), info)
        assertEquals("{\"id\":\"abc\"}", info.readText())
        assertEquals(info, bundle.metadataFile)
    }

    @Test
    fun `bundle includes jpg when already in export dir`() {
        val root = tmp.newFolder("export")
        File(root, "creator").mkdirs()
        File(root, "creator/abc.jpg").writeBytes(byteArrayOf(1, 2, 3))

        val bundle = buildRMetaBundle(root, "creator", "abc", infoJson = "{}")

        assertEquals(2, bundle.files.size)
        assertTrue(bundle.files.contains(File(root, "creator/abc.jpg")))
        assertTrue(bundle.files.contains(File(root, "creator/abc.info")))
        assertEquals(File(root, "creator/abc.info"), bundle.metadataFile)
    }
}
