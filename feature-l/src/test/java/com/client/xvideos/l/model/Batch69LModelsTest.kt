package com.client.xvideos.l.model

import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.AudiencesType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch69LModelsTest {

    @Test
    fun `AlbumType cyclic navigation and companions`() {
        assertEquals(AlbumType.Manga, AlbumType.All.next())
        assertEquals(AlbumType.Pictures, AlbumType.Manga.next())
        assertEquals(AlbumType.All, AlbumType.Pictures.next())

        assertEquals(AlbumType.Pictures, AlbumType.All.prev())
        assertEquals(AlbumType.All, AlbumType.Manga.prev())
        assertEquals(AlbumType.Manga, AlbumType.Pictures.prev())

        assertEquals(listOf("All", "Manga", "Pictures"), AlbumType.allTitles)

        assertEquals(AlbumType.All, AlbumType.fromOrdinalOrDefault(0))
        assertEquals(AlbumType.Manga, AlbumType.fromOrdinalOrDefault(1))
        assertEquals(AlbumType.Pictures, AlbumType.fromOrdinalOrDefault(2))
        assertEquals(AlbumType.Pictures, AlbumType.fromOrdinalOrDefault(99))
    }

    @Test
    fun `AudiencesType cyclic navigation and companions`() {
        assertEquals(AudiencesType.LESBIAN, AudiencesType.GAY.next())
        assertEquals(AudiencesType.GAY, AudiencesType.TRANS_X_TRANS.next())

        assertEquals(AudiencesType.TRANS_X_TRANS, AudiencesType.GAY.prev())
        assertEquals(AudiencesType.GAY, AudiencesType.LESBIAN.prev())

        assertEquals(AudiencesType.entries.map { it.title }, AudiencesType.allTitles)

        assertEquals(AudiencesType.GAY, AudiencesType.fromOrdinalOrDefault(0))
        assertEquals(AudiencesType.LESBIAN, AudiencesType.fromOrdinalOrDefault(1))
        assertEquals(AudiencesType.STRAIGHT, AudiencesType.fromOrdinalOrDefault(99))
    }

    @Test
    fun `Album matches and normalizedTitle`() {
        val album = Album(
            id = "100",
            title = "  Awesome Manga Strip  ",
            description = "A great adventure story",
            tags = listOf(Tag(text = "Adventure")),
            genres = listOf(Genre(title = "Fantasy"))
        )

        assertEquals("Awesome Manga Strip", album.normalizedTitle)
        assertTrue(album.matches(null))
        assertTrue(album.matches(""))
        assertTrue(album.matches("awesome"))
        assertTrue(album.matches("ADVENTURE"))
        assertTrue(album.matches("story"))
        assertTrue(album.matches("fantasy"))
        assertFalse(album.matches("sci-fi"))
    }

    @Test
    fun `User matches and hasUrl`() {
        val userWithoutUrl = User(id = "1", name = "john_doe", displayName = "John Doe")
        assertFalse(userWithoutUrl.hasUrl)
        assertTrue(userWithoutUrl.matches(null))
        assertTrue(userWithoutUrl.matches("john"))
        assertTrue(userWithoutUrl.matches("DOE"))
        assertFalse(userWithoutUrl.matches("alice"))

        val userWithUrl = userWithoutUrl.copy(url = "/users/john")
        assertTrue(userWithUrl.hasUrl)
    }

    @Test
    fun `Tag Genre Audience matches`() {
        val tag = Tag(id = "1", text = "Cosplay", category = "Costume")
        assertTrue(tag.matches(null))
        assertTrue(tag.matches("cosplay"))
        assertTrue(tag.matches("costume"))
        assertFalse(tag.matches("anime"))

        val genre = Genre(id = "2", title = "Romance")
        assertTrue(genre.matches(null))
        assertTrue(genre.matches("romance"))
        assertFalse(genre.matches("horror"))

        val audience = Audience(id = "3", title = "Straight")
        assertTrue(audience.matches(null))
        assertTrue(audience.matches("straight"))
        assertFalse(audience.matches("gay"))
    }

    @Test
    fun `AlbumDetails helpers and matches`() {
        val details = AlbumDetails(
            id = "200",
            title = "Test Album",
            description = "Some description",
            is_manga = false,
            number_of_pictures = 15,
            cover = Cover(url = "https://cdn/cover.jpg"),
            tags = listOf(Tag(text = "Hero")),
            genres = listOf(Genre(title = "Action"))
        )

        assertTrue(details.isNotManga)
        assertTrue(details.hasAnyPictures())
        assertTrue(details.hasCoverUrl())
        assertTrue(details.matches("test"))
        assertTrue(details.matches("hero"))
        assertTrue(details.matches("action"))
        assertFalse(details.matches("mystery"))

        val mangaDetails = details.copy(is_manga = true, number_of_pictures = 0, cover = null)
        assertFalse(mangaDetails.isNotManga)
        assertFalse(mangaDetails.hasAnyPictures())
        assertFalse(mangaDetails.hasCoverUrl())
    }

    @Test
    fun `PicsDetails and Thumbnails new helpers`() {
        val pic = PicsDetails(
            id = "555",
            width = 1920,
            height = 1080,
            is_animated = false,
            url_to_video = "https://cdn/video.mp4",
            url_to_original = "/local/path/pic.jpg"
        )

        assertTrue(pic.hasValidId)
        assertTrue(pic.isGifOrVideo)
        assertTrue(pic.isLocalFile())
        assertEquals(2073600L, pic.totalPixels())
        assertTrue(pic.hasValidMedia())

        val invalidPic = PicsDetails(id = "0")
        assertFalse(invalidPic.hasValidId)

        val thumb = Thumbnails(width = 640, height = 360, url = "https://cdn/thumb.jpg")
        assertTrue(thumb.hasDimensions)
        assertEquals(640f / 360f, thumb.aspectRatio, 0.001f)

        val zeroThumb = Thumbnails()
        assertFalse(zeroThumb.hasDimensions)
        assertEquals(0f, zeroThumb.aspectRatio, 0.001f)
    }

    @Test
    fun `FilterGenre matches and normalizedSlug`() {
        val filter = FilterGenre(
            id = "10",
            title = "3D Art",
            slug = "  3D-Art  ",
            description = "Rendered CG pictures"
        )

        assertEquals("3d-art", filter.normalizedSlug)
        assertTrue(filter.matches(null))
        assertTrue(filter.matches("3d"))
        assertTrue(filter.matches("art"))
        assertTrue(filter.matches("rendered"))
        assertFalse(filter.matches("vintage"))
    }

    @Test
    fun `UserProfile normalizedEmail and isEmailValid`() {
        val profile = UserProfile(email = "  Test.User@Example.COM  ", password = "secretPassword")
        assertEquals("test.user@example.com", profile.normalizedEmail)
        assertTrue(profile.isEmailValid)

        val invalidProfile = UserProfile(email = "invalid-email")
        assertFalse(invalidProfile.isEmailValid)

        val emptyProfile = UserProfile()
        assertFalse(emptyProfile.isEmailValid)
    }
}
