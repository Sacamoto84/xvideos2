package com.client.xvideos.common.p2p.export

import com.client.xvideos.common.p2p.P2pManifestFactory
import com.client.xvideos.common.p2p.P2pType
import com.client.xvideos.common.p2p.mirrorRoot
import com.client.xvideos.common.zip.ZipUtils
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Content
import com.client.xvideos.l.model.Cover
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private fun albumDetails(id: String): AlbumDetails = AlbumDetails(
        created = 0.0,
        modified = 0.0,
        id = id,
        title = "Test Album",
        tags = emptyList(),
        is_manga = false,
        content = Content(id = "", title = "", url = ""),
        genres = emptyList(),
        cover = Cover(width = 0, height = 0, size = "", url = ""),
        description = "",
        audiences = emptyList(),
        number_of_pictures = 0,
        number_of_animated_pictures = 0,
        url = "",
        download_url = "",
    )

    @Test
    fun `L album exporter uses saved file when album is saved`() {
        val main = tmp.newFolder("xvideos")
        val savedRoot = File(main, "L/Album").apply { mkdirs() }
        val outboxRoot = File(main, "outbox/L/Album")
        File(savedRoot, "42.album").writeText("{\"id\":\"42\"}")

        val bundle = LAlbumExporter.export(albumDetails("42"), savedRoot, outboxRoot)!!

        assertEquals(P2pType.L_ALBUM, bundle.type)
        assertEquals(savedRoot, bundle.storeRoot)
        assertEquals(listOf(File(savedRoot, "42.album")), bundle.files)
        assertEquals("42.album", bundle.metadataFile!!.name)
    }

    @Test
    fun `L album exporter writes gson file to outbox when not saved`() {
        val main = tmp.newFolder("xvideos")
        val savedRoot = File(main, "L/Album").apply { mkdirs() }
        val outboxRoot = File(main, "outbox/L/Album")

        val bundle = LAlbumExporter.export(albumDetails("42"), savedRoot, outboxRoot)!!

        assertEquals(P2pType.L_ALBUM, bundle.type)
        assertEquals(outboxRoot, bundle.storeRoot)
        val file = File(outboxRoot, "42.album")
        assertTrue(file.exists())
        // содержимое читается обратно как AlbumDetails (формат FileDB)
        val back = GsonBuilder().setPrettyPrinting().create()
            .fromJson(file.readText(), AlbumDetails::class.java)
        assertEquals("42", back.id)
        assertEquals("Test Album", back.title)
    }

    @Test
    fun `L album exporter returns null for invalid id`() {
        val main = tmp.newFolder("xvideos")
        assertNull(
            LAlbumExporter.export(
                albumDetails(""),
                File(main, "L/Album"),
                File(main, "outbox/L/Album"),
            )
        )
    }

    @Test
    fun `L collection exporter zips collection into outbox`() {
        val main = tmp.newFolder("xvideos")
        val collectionRoot = File(main, "L/Collection").apply { mkdirs() }
        val outboxDir = File(main, "outbox").apply { mkdirs() }
        val col = File(collectionRoot, "MyCol/item1").apply { mkdirs() }
        File(col, "media.jpg").writeText("M")
        File(col, "metadata.json").writeText("{}")

        val bundle = LCollectionExporter.export("MyCol", collectionRoot, outboxDir)!!

        assertEquals(P2pType.L_COLLECTION, bundle.type)
        assertEquals(outboxDir, bundle.storeRoot)
        val zip = bundle.files.single()
        assertEquals("MyCol.zip", zip.name)
        // содержимое архива воспроизводит коллекцию с именем-префиксом
        val check = File(main, "check").apply { mkdirs() }
        ZipUtils.unzip(zip, check)
        assertEquals("M", File(check, "MyCol/item1/media.jpg").readText())
    }

    @Test
    fun `L collection exporter returns null for missing or empty collection`() {
        val main = tmp.newFolder("xvideos")
        val collectionRoot = File(main, "L/Collection").apply { mkdirs() }
        val outboxDir = File(main, "outbox").apply { mkdirs() }

        // нет папки
        assertNull(LCollectionExporter.export("Nope", collectionRoot, outboxDir))
        // папка есть, но пустая
        File(collectionRoot, "Empty").mkdirs()
        assertNull(LCollectionExporter.export("Empty", collectionRoot, outboxDir))
    }
}
