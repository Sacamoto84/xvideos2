package com.client.xvideos.common.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class P2pStagingTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `mirrorRoot maps store root inside staging base`() {
        val main = tmp.newFolder("xvideos")
        val inbox = File(main, "inbox")
        val store = File(main, "L/Likes")

        assertEquals(File(inbox, "L/Likes"), mirrorRoot(inbox, main, store))
    }

    @Test
    fun `merge moves files into main keeping structure and empties inbox`() {
        val main = tmp.newFolder("xvideos")
        val inbox = File(main, "inbox").apply { mkdirs() }
        File(inbox, "L/Likes/item1").mkdirs()
        File(inbox, "L/Likes/item1/media.jpg").writeText("M")
        File(inbox, "L/Likes/item1/metadata.json").writeText("{}")
        File(inbox, "X/Download").mkdirs()
        File(inbox, "X/Download/7.mp4").writeText("V")

        P2pInboxMerger.merge(inbox, main)

        assertEquals("M", File(main, "L/Likes/item1/media.jpg").readText())
        assertEquals("{}", File(main, "L/Likes/item1/metadata.json").readText())
        assertEquals("V", File(main, "X/Download/7.mp4").readText())
        // inbox пересоздан пустым
        assertTrue(inbox.exists())
        assertTrue(inbox.listFiles().isNullOrEmpty())
    }

    @Test
    fun `merge overwrites existing files in main`() {
        val main = tmp.newFolder("xvideos")
        val inbox = File(main, "inbox").apply { mkdirs() }
        File(main, "X/Download").mkdirs()
        File(main, "X/Download/7.mp4").writeText("OLD")
        File(inbox, "X/Download").mkdirs()
        File(inbox, "X/Download/7.mp4").writeText("NEW")

        P2pInboxMerger.merge(inbox, main)

        assertEquals("NEW", File(main, "X/Download/7.mp4").readText())
    }

    @Test
    fun `merge on missing inbox is a no-op`() {
        val main = tmp.newFolder("xvideos")
        P2pInboxMerger.merge(File(main, "inbox"), main)
        // не упало — этого достаточно
    }
}
