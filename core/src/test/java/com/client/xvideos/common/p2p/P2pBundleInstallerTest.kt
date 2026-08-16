package com.client.xvideos.common.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class P2pBundleInstallerTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `installs received files into store root by relative path`() {
        val received1 = tmp.newFile("payload_100").apply { writeText("MEDIA") }
        val received2 = tmp.newFile("payload_101").apply { writeText("{\"k\":1}") }
        val storeRoot = tmp.newFolder("likes")

        val manifest = P2pManifest(
            type = P2pType.L,
            metadataFileName = "metadata.json",
            files = listOf(
                P2pManifestFile("media.jpg", "album_x/media.jpg", 100L, 5L),
                P2pManifestFile("metadata.json", "album_x/metadata.json", 101L, 7L),
            ),
        )

        val written = P2pBundleInstaller.install(
            storeRoot = storeRoot,
            manifest = manifest,
            receivedFiles = mapOf(100L to received1, 101L to received2),
        )

        val media = File(storeRoot, "album_x/media.jpg")
        val meta = File(storeRoot, "album_x/metadata.json")
        assertTrue(media.exists())
        assertEquals("MEDIA", media.readText())
        assertEquals("{\"k\":1}", meta.readText())
        assertEquals(setOf(media.canonicalPath, meta.canonicalPath), written.map { it.canonicalPath }.toSet())
    }

    @Test(expected = IllegalStateException::class)
    fun `throws when a payload is missing`() {
        val storeRoot = tmp.newFolder("likes")
        val manifest = P2pManifest(
            type = P2pType.X,
            metadataFileName = "5.info",
            files = listOf(P2pManifestFile("5.mp4", "5.mp4", 1L, 1L)),
        )
        P2pBundleInstaller.install(storeRoot, manifest, receivedFiles = emptyMap())
    }

    @Test
    fun `установщик отвергает путь за пределами store root`() {
        val received = tmp.newFile("payload_200").apply { writeText("EVIL") }
        val storeRoot = tmp.newFolder("likes_evil")
        val outside = File(storeRoot.parentFile, "stolen.txt")

        val manifest = P2pManifest(
            type = P2pType.L,
            metadataFileName = null,
            files = listOf(P2pManifestFile("a.jpg", "../stolen.txt", 200L, 4L)),
        )

        try {
            P2pBundleInstaller.install(storeRoot, manifest, mapOf(200L to received))
            fail("Ожидался отказ на пути за пределами store root")
        } catch (e: IllegalArgumentException) {
            // ожидаемо
        }
        assertFalse("файл не должен появиться снаружи store root", outside.exists())
    }
}
