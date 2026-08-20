package com.client.xvideos.r.common.p2p

import com.client.xvideos.common.p2p.P2pManifest
import com.client.xvideos.common.p2p.P2pManifestFile
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.r.model.GifsInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RLikesBundleImporterTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `parses info from bundle and adds like`() = runTest {
        val info = tmp.newFile("abc.info").apply {
            writeText("""{"id":"abc","userName":"creator","likes":5}""")
        }
        val manifest = P2pManifest(
            type = P2pType.R,
            metadataFileName = "abc.info",
            files = listOf(P2pManifestFile("abc.info", "creator/abc.info", 42L, info.length())),
        )

        var added: GifsInfo? = null
        val importer = RLikesBundleImporter(addLike = { added = it })

        importer.import(manifest, mapOf(42L to info))

        assertEquals("abc", added!!.id)
        assertEquals("creator", added!!.userName)
    }
}
