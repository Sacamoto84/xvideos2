package com.client.xvideos.l.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch61LModelsTest {

    @Test
    fun `AlbumDetails inspection and lookup helpers`() {
        val empty = AlbumDetails.EMPTY
        assertEquals(0L, empty.numericId)
        assertFalse(empty.isLiked)
        assertEquals(0, empty.tagsCount)
        assertEquals(0, empty.genresCount)
        assertEquals(0, empty.audiencesCount)
        assertFalse(empty.containsTag("tag1"))
        assertFalse(empty.containsGenre("genre1"))

        val album = AlbumDetails(
            id = "12345",
            likeStatus = "like",
            tags = listOf(Tag(text = "Cosplay"), Tag(text = "Fantasy")),
            genres = listOf(Genre(title = "Romance"), Genre(title = "Action")),
            audiences = listOf(Audience(title = "General"))
        )
        assertEquals(12345L, album.numericId)
        assertTrue(album.isLiked)
        assertEquals(2, album.tagsCount)
        assertEquals(2, album.genresCount)
        assertEquals(1, album.audiencesCount)

        assertTrue(album.containsTag("cosplay"))
        assertTrue(album.containsTag("FANTASY"))
        assertFalse(album.containsTag("Sci-Fi"))
        assertFalse(album.containsTag(null))
        assertFalse(album.containsTag(""))

        assertTrue(album.containsGenre("romance"))
        assertTrue(album.containsGenre("ACTION"))
        assertFalse(album.containsGenre("Comedy"))
        assertFalse(album.containsGenre(null))
        assertFalse(album.containsGenre(""))
    }

    @Test
    fun `PicsDetails and Thumbnails inspection helpers`() {
        val empty = PicsDetails.EMPTY
        assertEquals(0L, empty.numericId)
        assertEquals(0, empty.thumbnailsCount)
        assertFalse(empty.hasDimensions)
        assertEquals(0f, empty.aspectRatio, 0.001f)
        assertNull(empty.findThumbnailBySizeOrNull("small"))

        val thumbSmall = Thumbnails(width = 300, height = 200, size = "small", url = "https://cdn/small.jpg")
        val thumbLarge = Thumbnails(width = 1200, height = 800, size = "xMax", url = "https://cdn/large.jpg")
        assertTrue(thumbSmall.hasUrl)
        assertTrue(thumbSmall.hasSize)

        val pic = PicsDetails(
            id = "9876",
            width = 1200,
            height = 600,
            thumbnails = listOf(thumbSmall, thumbLarge)
        )
        assertEquals(9876L, pic.numericId)
        assertEquals(2, pic.thumbnailsCount)
        assertTrue(pic.hasDimensions)
        assertEquals(2.0f, pic.aspectRatio, 0.001f)

        val foundSmall = pic.findThumbnailBySizeOrNull("small")
        assertNotNull(foundSmall)
        assertEquals(300, foundSmall?.width)

        val foundLarge = pic.findThumbnailBySizeOrNull("XMAX")
        assertNotNull(foundLarge)
        assertEquals(1200, foundLarge?.width)

        assertNull(pic.findThumbnailBySizeOrNull("medium"))
        assertNull(pic.findThumbnailBySizeOrNull(null))
        assertNull(pic.findThumbnailBySizeOrNull(""))
    }

    @Test
    fun `PicsDetailsMedia utility functions`() {
        val staticPic = PicsDetails(
            id = "1",
            is_animated = false,
            url_to_original = "https://cdn/pic.jpg",
            thumbnails = listOf(Thumbnails(size = "small", url = "https://cdn/pic_small.jpg"))
        )
        assertTrue(staticPic.isStaticImage())
        assertTrue(staticPic.hasValidDownloadUrl())
        assertTrue(staticPic.hasValidPreviewImage("small"))

        val emptyPic = PicsDetails.EMPTY
        assertTrue(emptyPic.isStaticImage())
        assertFalse(emptyPic.hasValidDownloadUrl())
        assertFalse(emptyPic.hasValidPreviewImage("small"))
    }

    @Test
    fun `UserProfile maskedPassword and isSameUser`() {
        val emptyUser = UserProfile.EMPTY
        assertEquals("", emptyUser.maskedPassword)
        assertFalse(emptyUser.isSameUser(emptyUser))

        val user = UserProfile(email = "test@example.com", password = "secret_password_123")
        assertEquals("********", user.maskedPassword)

        val sameUser = UserProfile(email = "TEST@EXAMPLE.COM", password = "other_password")
        assertTrue(user.isSameUser(sameUser))

        val otherUser = UserProfile(email = "other@example.com")
        assertFalse(user.isSameUser(otherUser))
        assertFalse(user.isSameUser(null))
    }

    @Test
    fun `FilterGenre hasUploadingRules and isSameGenre`() {
        val emptyGenre = FilterGenre.EMPTY
        assertFalse(emptyGenre.hasUploadingRules)
        assertFalse(emptyGenre.isSameGenre(emptyGenre))

        val genre = FilterGenre(id = "genre_1", title = "Manga", uploadingRules = "Must be high resolution")
        assertTrue(genre.hasUploadingRules)

        val sameGenre = FilterGenre(id = "genre_1", title = "Different Title")
        assertTrue(genre.isSameGenre(sameGenre))

        val differentGenre = FilterGenre(id = "genre_2")
        assertFalse(genre.isSameGenre(differentGenre))
        assertFalse(genre.isSameGenre(null))
    }

    @Test
    fun `SavedAlbumFilter inspection and comparison helpers`() {
        val preset = SavedAlbumFilter(id = "preset_uuid", name = "My Preset")
        assertTrue(preset.hasValidId)
        assertTrue(preset.hasName)

        val samePreset = SavedAlbumFilter(id = "preset_uuid", name = "Renamed Preset")
        assertTrue(preset.isSamePreset(samePreset))

        val differentPreset = SavedAlbumFilter(id = "other_uuid")
        assertFalse(preset.isSamePreset(differentPreset))
        assertFalse(preset.isSamePreset(null))

        val empty = SavedAlbumFilter(id = "", name = "")
        assertFalse(empty.hasValidId)
        assertFalse(empty.hasName)
    }

    @Test
    fun `Landing_page_albumType and Landing_page_albumSection helpers`() {
        val emptyLanding = Landing_page_albumType.EMPTY
        assertEquals(0, emptyLanding.sectionsCount)
        assertEquals(0, emptyLanding.totalItemsCount)
        assertNull(emptyLanding.firstOrNull)
        assertNull(emptyLanding.findSectionByTitleOrNull("any"))

        val album1 = Album(id = "a1", title = "Album 1")
        val album2 = Album(id = "a2", title = "Album 2")
        val section1 = Landing_page_albumSection(title = "Featured", url = "/featured", items = listOf(album1, album2))
        val section2 = Landing_page_albumSection(title = "Recent", items = listOf(Album(id = "a3")))

        assertTrue(section1.hasTitle)
        assertTrue(section1.hasUrl)
        assertEquals(2, section1.itemsCount)
        assertEquals(album1, section1.firstOrNull)

        val landing = Landing_page_albumType(title = "Explore", sections = listOf(section1, section2))
        assertEquals(2, landing.sectionsCount)
        assertEquals(3, landing.totalItemsCount)
        assertEquals(section1, landing.firstOrNull)

        val foundSection = landing.findSectionByTitleOrNull("featured")
        assertNotNull(foundSection)
        assertEquals("Featured", foundSection?.title)
        assertNull(landing.findSectionByTitleOrNull("nonexistent"))
        assertNull(landing.findSectionByTitleOrNull(null))
        assertNull(landing.findSectionByTitleOrNull(""))
    }
}
