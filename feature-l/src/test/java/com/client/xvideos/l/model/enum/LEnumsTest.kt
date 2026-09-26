package com.client.xvideos.l.model.enum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LEnumsTest {

    @Test
    fun `AlbumType properties and parser fallbacks`() {
        assertEquals(AlbumType.Pictures, AlbumType.DEFAULT)
        assertTrue(AlbumType.Pictures.isPictures)
        assertFalse(AlbumType.Pictures.isManga)
        assertFalse(AlbumType.Pictures.isAll)

        assertTrue(AlbumType.Manga.isManga)
        assertTrue(AlbumType.All.isAll)

        assertEquals(AlbumType.Manga, AlbumType.fromValue("manga"))
        assertEquals(AlbumType.Manga, AlbumType.fromValue("MANGA"))
        assertEquals(AlbumType.Pictures, AlbumType.fromValue(null))
        assertEquals(AlbumType.Pictures, AlbumType.fromValue("nonexistent"))
        assertEquals(AlbumType.All, AlbumType.fromValue("nonexistent", default = AlbumType.All))
        assertNull(AlbumType.fromValueOrNull("unknown"))
        assertNull(AlbumType.fromValueOrNull(null))
    }

    @Test
    fun `ContentId properties and parsers`() {
        assertEquals(ContentId.All, ContentId.DEFAULT)
        assertTrue(ContentId.All.isAll)
        assertFalse(ContentId.All.isHentai)

        assertTrue(ContentId.Hentai.isHentai)
        assertTrue(ContentId.NonErotic.isNonErotic)
        assertTrue(ContentId.RealPeople.isRealPeople)

        assertEquals(ContentId.Hentai, ContentId.fromValue(2))
        assertEquals(ContentId.All, ContentId.fromValue(null))
        assertEquals(ContentId.All, ContentId.fromValue(999))
        assertEquals(ContentId.NonErotic, ContentId.fromValue(999, default = ContentId.NonErotic))
        assertNull(ContentId.fromValueOrNull(999))
        assertNull(ContentId.fromValueOrNull(null))

        assertEquals(ContentId.RealPeople, ContentId.fromStringOrNull("6"))
        assertNull(ContentId.fromStringOrNull("abc"))
        assertNull(ContentId.fromStringOrNull(null))
    }

    @Test
    fun `PictureCountRank properties and parsers`() {
        assertEquals(PictureCountRank.All, PictureCountRank.DEFAULT)
        assertTrue(PictureCountRank.All.isAll)
        assertFalse(PictureCountRank.All.isSpecific)

        assertTrue(PictureCountRank.C0_25.isSpecific)
        assertFalse(PictureCountRank.C0_25.isAll)

        assertEquals(PictureCountRank.C25_50, PictureCountRank.fromCount(1))
        assertEquals(PictureCountRank.All, PictureCountRank.fromCount(null))
        assertEquals(PictureCountRank.All, PictureCountRank.fromCount(999))
        assertEquals(PictureCountRank.C100_200, PictureCountRank.fromCount(999, default = PictureCountRank.C100_200))
        assertNull(PictureCountRank.fromCountOrNull(999))
        assertNull(PictureCountRank.fromCountOrNull(null))

        assertEquals(PictureCountRank.C50_100, PictureCountRank.fromStringOrNull("2"))
        assertNull(PictureCountRank.fromStringOrNull("xyz"))
        assertNull(PictureCountRank.fromStringOrNull(null))
    }

    @Test
    fun `AudiencesType properties and lookups`() {
        val gay = AudiencesType.GAY
        assertTrue(gay.hasDescription)
        assertFalse(gay.hasPoster)

        val soloGuy = AudiencesType.SOLO_GUY
        assertFalse(soloGuy.hasDescription)

        assertEquals(AudiencesType.TRANS, AudiencesType.fromId(5))
        assertEquals(AudiencesType.TRANS, AudiencesType.fromIdOrNull(5))
        assertNull(AudiencesType.fromIdOrNull(999))
        assertNull(AudiencesType.fromIdOrNull(null))

        assertEquals(AudiencesType.LESBIAN, AudiencesType.fromUrl("/audiences/lesbian_3/"))
        assertEquals(AudiencesType.LESBIAN, AudiencesType.fromUrlOrNull("/audiences/lesbian_3/"))
        assertNull(AudiencesType.fromUrlOrNull("/audiences/unknown/"))
        assertNull(AudiencesType.fromUrlOrNull(null))
        assertNull(AudiencesType.fromUrlOrNull(""))
    }

    @Test
    fun `SelectIndex properties and parsers`() {
        assertEquals(SelectIndex.Default, SelectIndex.DEFAULT)
        assertTrue(SelectIndex.Unselect.isUnselect)
        assertFalse(SelectIndex.Unselect.isSelected)

        assertTrue(SelectIndex.Default.isSelected)
        assertTrue(SelectIndex.Default.isDefault)
        assertTrue(SelectIndex.Manga.isManga)
        assertTrue(SelectIndex.Hentai.isHentai)
        assertTrue(SelectIndex.Porn.isPorn)

        assertEquals(SelectIndex.Manga, SelectIndex.fromValue(1))
        assertEquals(SelectIndex.Default, SelectIndex.fromValue(null))
        assertEquals(SelectIndex.Default, SelectIndex.fromValue(999))
        assertEquals(SelectIndex.Porn, SelectIndex.fromValue(999, default = SelectIndex.Porn))
        assertNull(SelectIndex.fromValueOrNull(999))
        assertNull(SelectIndex.fromValueOrNull(null))

        assertEquals(SelectIndex.Hentai, SelectIndex.fromStringOrNull("2"))
        assertNull(SelectIndex.fromStringOrNull("foo"))
        assertNull(SelectIndex.fromStringOrNull(null))
    }
}
