package com.client.xvideos.l

import com.client.xvideos.l.model.LusciousLanguage
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.UserProfile
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.AudiencesType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import com.client.xvideos.l.model.enum.SelectIndex
import com.client.xvideos.l.model.hasDimensions
import com.client.xvideos.l.model.isLandscape
import com.client.xvideos.l.model.isPortrait
import com.client.xvideos.l.model.isSquare
import com.client.xvideos.l.model.resolutionString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch77LEnumsAndModelsTest {

    @Test
    fun testAlbumTypeHelpers() {
        assertEquals(listOf("All", "Manga", "Pictures"), AlbumType.allNames)
        assertEquals(listOf("all", "manga", "pictures"), AlbumType.allValues)
        assertTrue(AlbumType.Manga.isSpecificType)
        assertTrue(AlbumType.Pictures.isSpecificType)
        assertFalse(AlbumType.All.isSpecificType)
    }

    @Test
    fun testAudiencesTypeHelpers() {
        assertTrue(AudiencesType.allIds.contains(1))
        assertTrue(AudiencesType.allNames.contains("STRAIGHT"))
        assertTrue(AudiencesType.TRANS.isTransAudience)
        assertTrue(AudiencesType.TRANS_X_GIRL.isTransAudience)
        assertTrue(AudiencesType.STRAIGHT.isStraight)
        assertFalse(AudiencesType.STRAIGHT.isTransAudience)
        assertTrue(AudiencesType.GAY.isGayOrLesbian)
        assertTrue(AudiencesType.LESBIAN.isGayOrLesbian)
        assertFalse(AudiencesType.SOLO_GIRL.isGayOrLesbian)

        assertEquals(AudiencesType.STRAIGHT, AudiencesType.fromNameOrNull("straight"))
        assertEquals(AudiencesType.GAY, AudiencesType.fromNameOrNull("GAY"))
        assertNull(AudiencesType.fromNameOrNull("unknown_category"))
        assertEquals(AudiencesType.STRAIGHT, AudiencesType.fromNameOrDefault("unknown_category"))
        assertEquals(AudiencesType.LESBIAN, AudiencesType.fromNameOrDefault("unknown_category", AudiencesType.LESBIAN))
    }

    @Test
    fun testContentIdHelpers() {
        assertEquals(listOf(0, 2, 5, 6), ContentId.allValues)
        assertEquals(listOf("All", "Hentai", "Non-Erotic", "Real People"), ContentId.allTitles)
        assertEquals(listOf("All", "Hentai", "NonErotic", "RealPeople"), ContentId.allNames)
        assertFalse(ContentId.All.isSpecific)
        assertTrue(ContentId.Hentai.isSpecific)
        assertTrue(ContentId.NonErotic.isSpecific)
        assertTrue(ContentId.RealPeople.isSpecific)

        assertEquals(ContentId.Hentai, ContentId.All.next())
        assertEquals(ContentId.RealPeople, ContentId.All.prev())
        assertEquals(ContentId.All, ContentId.RealPeople.next())

        assertEquals(ContentId.All, ContentId.fromOrdinalOrDefault(0))
        assertEquals(ContentId.RealPeople, ContentId.fromOrdinalOrDefault(3))
        assertEquals(ContentId.All, ContentId.fromOrdinalOrDefault(99))
    }

    @Test
    fun testPictureCountRankHelpers() {
        assertEquals(8, PictureCountRank.allCounts.size)
        assertEquals(8, PictureCountRank.allRanges.size)
        assertEquals(8, PictureCountRank.allNames.size)

        assertEquals(PictureCountRank.C0_25, PictureCountRank.All.next())
        assertEquals(PictureCountRank.C3200_12800, PictureCountRank.All.prev())
        assertEquals(PictureCountRank.All, PictureCountRank.C3200_12800.next())

        assertEquals(PictureCountRank.All, PictureCountRank.fromOrdinalOrDefault(0))
        assertEquals(PictureCountRank.C3200_12800, PictureCountRank.fromOrdinalOrDefault(7))
        assertEquals(PictureCountRank.All, PictureCountRank.fromOrdinalOrDefault(100))
    }

    @Test
    fun testSelectIndexHelpers() {
        assertEquals(listOf(-1, 0, 1, 2, 3), SelectIndex.allValues)
        assertEquals(listOf("Unselect", "Default", "Manga", "Hentai", "Porn"), SelectIndex.allTitles)
        assertEquals(listOf("Unselect", "Default", "Manga", "Hentai", "Porn"), SelectIndex.allNames)

        assertEquals(SelectIndex.Default, SelectIndex.Unselect.next())
        assertEquals(SelectIndex.Porn, SelectIndex.Unselect.prev())
        assertEquals(SelectIndex.Unselect, SelectIndex.Porn.next())

        assertEquals(SelectIndex.Unselect, SelectIndex.fromOrdinalOrDefault(0))
        assertEquals(SelectIndex.Default, SelectIndex.fromOrdinalOrDefault(99))
    }

    @Test
    fun testLusciousLanguageEnum() {
        assertEquals(13, LusciousLanguage.entries.size)
        assertEquals(13, LusciousLanguage.allIds.size)
        assertEquals(13, LusciousLanguage.allTitles.size)
        assertEquals(13, LusciousLanguage.allNames.size)

        assertTrue(LusciousLanguage.ENGLISH.isEnglish)
        assertTrue(LusciousLanguage.RUSSIAN.isRussian)
        assertTrue(LusciousLanguage.JAPANESE.isJapanese)
        assertTrue(LusciousLanguage.NO_WORDS.isNoWords)

        assertEquals(LusciousLanguage.PORTUGUESE, LusciousLanguage.ENGLISH.next())
        assertEquals(LusciousLanguage.OTHER, LusciousLanguage.ENGLISH.prev())

        assertEquals(LusciousLanguage.ENGLISH, LusciousLanguage.fromIdOrNull(1))
        assertEquals(LusciousLanguage.RUSSIAN, LusciousLanguage.fromIdOrNull(7))
        assertNull(LusciousLanguage.fromIdOrNull(9999))
        assertEquals(LusciousLanguage.ENGLISH, LusciousLanguage.fromIdOrDefault(9999))

        assertEquals(LusciousLanguage.ENGLISH, LusciousLanguage.fromTitleOrNull("English"))
        assertEquals(LusciousLanguage.RUSSIAN, LusciousLanguage.fromTitleOrDefault("Russian / Pусский"))

        assertEquals(LusciousLanguage.ENGLISH, LusciousLanguage.fromStringOrNull("1"))
        assertEquals(LusciousLanguage.ENGLISH, LusciousLanguage.fromStringOrNull("ENGLISH"))
        assertEquals(LusciousLanguage.ENGLISH, LusciousLanguage.fromStringOrDefault("invalid"))

        assertEquals(LusciousLanguage.ENGLISH, LusciousLanguage.fromOrdinalOrDefault(0))
        assertEquals(LusciousLanguage.ENGLISH, LusciousLanguage.fromOrdinalOrDefault(99))
    }

    @Test
    fun testPicsDetailsMediaDimensions() {
        val portrait = PicsDetails(width = 800, height = 1200)
        assertTrue(portrait.hasDimensions())
        assertTrue(portrait.isPortrait())
        assertFalse(portrait.isLandscape())
        assertFalse(portrait.isSquare())
        assertEquals("800x1200", portrait.resolutionString())

        val landscape = PicsDetails(width = 1920, height = 1080)
        assertTrue(landscape.hasDimensions())
        assertFalse(landscape.isPortrait())
        assertTrue(landscape.isLandscape())
        assertFalse(landscape.isSquare())
        assertEquals("1920x1080", landscape.resolutionString())

        val square = PicsDetails(width = 500, height = 500)
        assertTrue(square.hasDimensions())
        assertFalse(square.isPortrait())
        assertFalse(square.isLandscape())
        assertTrue(square.isSquare())
        assertEquals("500x500", square.resolutionString())

        val noDim = PicsDetails(width = 0, height = 0)
        assertFalse(noDim.hasDimensions())
        assertFalse(noDim.isPortrait())
        assertFalse(noDim.isLandscape())
        assertFalse(noDim.isSquare())
        assertEquals("", noDim.resolutionString())
    }

    @Test
    fun testUserProfileHelpers() {
        val user = UserProfile(email = "alice@example.com", password = "secretPassword123")
        assertEquals("alice", user.usernamePart)
        assertEquals("example.com", user.domainPart)
        assertTrue(user.isEmailValid)

        val updatedPass = user.withPassword("newSecret456")
        assertEquals("newSecret456", updatedPass.password)
        assertEquals("alice@example.com", updatedPass.email)

        val updatedEmail = user.withEmail("bob@example.com")
        assertEquals("bob@example.com", updatedEmail.email)
        assertEquals("secretPassword123", updatedEmail.password)

        val cleared = user.clear()
        assertTrue(cleared.isEmpty)
        assertEquals("", cleared.email)
        assertEquals("", cleared.password)

        val noAtUser = UserProfile(email = "simpleUser", password = "pass")
        assertEquals("simpleUser", noAtUser.usernamePart)
        assertEquals("", noAtUser.domainPart)
        assertFalse(noAtUser.isEmailValid)
    }
}
