package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.p2p.P2pType
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

class LAlbumExporterTest {

    @get:Rule val tmp = TemporaryFolder()

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
}
