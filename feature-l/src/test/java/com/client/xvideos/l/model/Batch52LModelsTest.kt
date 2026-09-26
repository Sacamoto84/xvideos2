package com.client.xvideos.l.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch52LModelsTest {

    @Test
    fun `AlbumDetails inspection properties and effectiveTitle fallback`() {
        val empty = AlbumDetails.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasTags)
        assertFalse(empty.hasGenres)
        assertFalse(empty.hasAudiences)
        assertFalse(empty.hasDescription)
        assertFalse(empty.hasLanguage)
        assertFalse(empty.hasCreatedBy)
        assertEquals("", empty.effectiveTitle)

        val album = AlbumDetails(
            id = "12345",
            title = "",
            tags = listOf(Tag(id = "1", text = "Tag")),
            genres = listOf(Genre(id = "2", title = "Genre")),
            audiences = listOf(Audience(id = "3", title = "Audience")),
            description = "Some description",
            language = Language(id = "en", title = "English"),
            createdBy = User(id = "u1", name = "Creator")
        )
        assertTrue(album.isValid)
        assertTrue(album.hasTags)
        assertTrue(album.hasGenres)
        assertTrue(album.hasAudiences)
        assertTrue(album.hasDescription)
        assertTrue(album.hasLanguage)
        assertTrue(album.hasCreatedBy)
        assertEquals("12345", album.effectiveTitle)

        val withTitle = album.copy(title = "Real Title")
        assertEquals("Real Title", withTitle.effectiveTitle)
    }

    @Test
    fun `Content inspection properties`() {
        val empty = Content.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasTitle)

        val content = Content(id = "c1", title = "Comic")
        assertTrue(content.isValid)
        assertTrue(content.hasTitle)
    }

    @Test
    fun `Cover dimensions and aspect ratio calculations`() {
        val empty = Cover.EMPTY
        assertFalse(empty.isValid)
        assertFalse(empty.hasDimensions)
        assertEquals(0f, empty.aspectRatio, 0.001f)

        val cover = Cover(width = 800, height = 600, url = "https://cdn/cover.jpg")
        assertTrue(cover.isValid)
        assertTrue(cover.hasDimensions)
        assertEquals(800f / 600f, cover.aspectRatio, 0.001f)
    }

    @Test
    fun `Album inspection properties`() {
        val empty = Album.EMPTY
        assertFalse(empty.hasDescription)
        assertFalse(empty.hasDownloadUrl)
        assertFalse(empty.hasFavorites)
        assertFalse(empty.hasLanguage)

        val album = Album(
            id = "101",
            title = "Album Title",
            description = "Story",
            downloadUrl = "/download/101",
            numberOfFavorites = 15,
            language = Language(id = "ja", title = "Japanese")
        )
        assertTrue(album.hasDescription)
        assertTrue(album.hasDownloadUrl)
        assertTrue(album.hasFavorites)
        assertTrue(album.hasLanguage)
    }

    @Test
    fun `FacetCollectionInfo page position flags`() {
        val page1Of5 = FacetCollectionInfo(page = 1, hasNextPage = true, totalPages = 5)
        assertTrue(page1Of5.isFirstPage)
        assertFalse(page1Of5.isLastPage)
        assertTrue(page1Of5.hasMultiplePages)

        val page5Of5 = FacetCollectionInfo(page = 5, hasNextPage = false, totalPages = 5)
        assertFalse(page5Of5.isFirstPage)
        assertTrue(page5Of5.isLastPage)
        assertTrue(page5Of5.hasMultiplePages)

        val singlePage = FacetCollectionInfo(page = 1, hasNextPage = false, totalPages = 1)
        assertTrue(singlePage.isFirstPage)
        assertTrue(singlePage.isLastPage)
        assertFalse(singlePage.hasMultiplePages)
    }

    @Test
    fun `UserProfile configuration flags`() {
        val empty = UserProfile.EMPTY
        assertFalse(empty.isConfigured)
        assertFalse(empty.hasCredentials)

        val configured = UserProfile(email = "test@example.com", password = "secret_password")
        assertTrue(configured.isConfigured)
        assertTrue(configured.hasCredentials)
    }
}
