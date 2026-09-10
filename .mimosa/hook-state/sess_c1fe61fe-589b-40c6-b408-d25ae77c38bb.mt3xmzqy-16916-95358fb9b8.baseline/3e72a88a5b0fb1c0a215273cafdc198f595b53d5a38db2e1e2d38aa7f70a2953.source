package com.client.xvideos.common.p2p.imports

import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pManifestFile
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.zip.ZipUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LCollectionBundleImporterTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `unzips received archive into collection store and merges`() = runTest {
        val main = tmp.newFolder("xvideos")
        val inbox = File(main, "inbox").apply { mkdirs() }
        val collectionStore = File(main, "L/Collection").apply { mkdirs() }

        // Готовим zip коллекции "MyCol" (как его собрал бы отправитель).
        val srcRoot = tmp.newFolder("src")
        val srcCol = File(srcRoot, "MyCol/item1").apply { mkdirs() }
        File(srcCol, "media.jpg").writeText("M")
        File(srcCol, "metadata.json").writeText("{}")
        val zip = File(tmp.newFolder("zipdir"), "MyCol.zip")
        ZipUtils.zipDirectory(File(srcRoot, "MyCol"), zip)

        var refreshed = false
        val importer = LCollectionBundleImporter(
            inboxRoot = inbox,
            mainRoot = main,
            collectionStoreRoot = collectionStore,
            refresh = { refreshed = true },
        )

        importer.import(
            P2pManifest(
                type = P2pType.L_COLLECTION,
                metadataFileName = null,
                files = listOf(P2pManifestFile("MyCol.zip", "MyCol.zip", 1L, zip.length())),
            ),
            mapOf(1L to zip),
        )

        assertEquals("M", File(collectionStore, "MyCol/item1/media.jpg").readText())
        assertEquals("{}", File(collectionStore, "MyCol/item1/metadata.json").readText())
        assertTrue(inbox.listFiles().isNullOrEmpty())
        assertTrue(refreshed)
    }
}
