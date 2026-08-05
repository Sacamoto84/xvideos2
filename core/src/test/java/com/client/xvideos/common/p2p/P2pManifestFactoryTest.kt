package com.client.xvideos.common.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class P2pManifestFactoryTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `relative paths are computed from store root and payload ids attached`() {
        val root = tmp.newFolder("likes")
        val folder = File(root, "album_x").apply { mkdirs() }
        val media = File(folder, "media.jpg").apply { writeText("aaa") }
        val meta = File(folder, "metadata.json").apply { writeText("{}") }

        val manifest = P2pManifestFactory.create(
            type = P2pType.L,
            storeRoot = root,
            files = listOf(media, meta),
            metadataFile = meta,
            payloadIds = mapOf(media to 100L, meta to 101L),
        )

        assertEquals(P2pType.L, manifest.type)
        assertEquals("metadata.json", manifest.metadataFileName)
        val byName = manifest.files.associateBy { it.name }
        assertEquals("album_x/media.jpg", byName.getValue("media.jpg").relativePath)
        assertEquals(100L, byName.getValue("media.jpg").payloadId)
        assertEquals(3L, byName.getValue("media.jpg").size)
        assertEquals("album_x/metadata.json", byName.getValue("metadata.json").relativePath)
    }

    @Test
    fun `flat store layout yields bare file names as relative paths`() {
        val root = tmp.newFolder("download")
        val mp4 = File(root, "555.mp4").apply { writeText("v") }
        val info = File(root, "555.info").apply { writeText("{}") }

        val manifest = P2pManifestFactory.create(
            type = P2pType.X,
            storeRoot = root,
            files = listOf(mp4, info),
            metadataFile = info,
            payloadIds = mapOf(mp4 to 1L, info to 2L),
        )

        val byName = manifest.files.associateBy { it.name }
        assertEquals("555.mp4", byName.getValue("555.mp4").relativePath)
        assertTrue(manifest.files.all { !it.relativePath.contains('\\') })
    }
}
