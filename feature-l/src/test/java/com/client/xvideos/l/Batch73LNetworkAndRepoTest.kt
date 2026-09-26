package com.client.xvideos.l

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.net.LAlbumBundleCache
import com.client.xvideos.l.net.extractAlbumIdOrNull
import com.client.xvideos.l.net.isValidAlbumUrl
import com.client.xvideos.l.repository.AlbumResult
import com.client.xvideos.l.repository.LusciousEndpoints
import com.client.xvideos.l.repository.RepositoryAction
import com.client.xvideos.l.repository.RepositoryResult
import com.client.xvideos.l.repository.albumIdOrNull
import com.client.xvideos.l.repository.albumTitleOrEmpty
import com.client.xvideos.l.repository.fold
import com.client.xvideos.l.repository.hasAlbumInfo
import com.client.xvideos.l.repository.isAlbums
import com.client.xvideos.l.repository.isEmpty
import com.client.xvideos.l.repository.isError
import com.client.xvideos.l.repository.isLoadAlbum
import com.client.xvideos.l.repository.isLoading
import com.client.xvideos.l.repository.isLogin
import com.client.xvideos.l.repository.isNotEmpty
import com.client.xvideos.l.repository.isSuccess
import com.client.xvideos.l.repository.recover
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch73LNetworkAndRepoTest {

    @Test
    fun `AlbumResult empty and helper properties`() {
        val empty = AlbumResult.Empty
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)
        assertFalse(empty.isAlbums)
        assertFalse(empty.hasAlbumInfo)
        assertNull(empty.albumIdOrNull)
        assertEquals("", empty.albumTitleOrEmpty)
    }

    @Test
    fun `RepositoryResult fold and recover`() {
        val success: RepositoryResult = RepositoryResult.Success("testData")
        val foldedSuccess = success.fold<String, String>(
            onSuccess = { it },
            onError = { msg, _ -> "error: $msg" },
            onLoading = { "loading" }
        )
        assertEquals("testData", foldedSuccess)
        assertTrue(success.isSuccess)
        assertFalse(success.isError)
        assertFalse(success.isLoading)

        val error: RepositoryResult = RepositoryResult.Error("Network failure")
        val foldedError = error.fold<String, String>(
            onSuccess = { "success" },
            onError = { msg, _ -> msg },
            onLoading = { "loading" }
        )
        assertEquals("Network failure", foldedError)
        assertTrue(error.isError)

        val recovered = error.recover("fallbackData")
        assertTrue(recovered.isSuccess)
        assertEquals("fallbackData", (recovered as RepositoryResult.Success<*>).data)

        val loading: RepositoryResult = RepositoryResult.Loading
        assertTrue(loading.isLoading)
        val foldedLoading = loading.fold<String, String>(
            onSuccess = { "s" },
            onError = { _, _ -> "e" },
            onLoading = { "loadingState" }
        )
        assertEquals("loadingState", foldedLoading)
    }

    @Test
    fun `LusciousEndpoints url formatters and verifiers`() {
        val albumUrl = LusciousEndpoints.albumUrl("comic-strip", "12345")
        assertEquals("https://members.luscious.net/albums/comic-strip_12345/", albumUrl)

        assertTrue(LusciousEndpoints.isLusciousUrl("https://members.luscious.net/albums/"))
        assertTrue(LusciousEndpoints.isLusciousUrl("https://luscious.net/something"))
        assertFalse(LusciousEndpoints.isLusciousUrl("https://otherdomain.com"))
        assertFalse(LusciousEndpoints.isLusciousUrl(null))
        assertFalse(LusciousEndpoints.isLusciousUrl(""))

        assertTrue(LusciousEndpoints.isApiUrl("https://members.luscious.net/graphql/nobatch/"))
        assertFalse(LusciousEndpoints.isApiUrl("https://members.luscious.net/other/"))
    }

    @Test
    fun `RepositoryAction action predicates and albumId`() {
        val loadAction: RepositoryAction = RepositoryAction.LoadAlbum(98765L)
        assertTrue(loadAction.isLoadAlbum)
        assertFalse(loadAction.isLogin)
        assertEquals(98765L, loadAction.albumIdOrNull)

        val loginAction: RepositoryAction = RepositoryAction.Login
        assertTrue(loginAction.isLogin)
        assertFalse(loginAction.isLoadAlbum)
        assertNull(loginAction.albumIdOrNull)
    }

    @Test
    fun `LAlbumBundleCache age helpers and hasAlbum`() {
        val now = 1_000_000L
        val cache = LAlbumBundleCache(
            cachedAtMs = 900_000L,
            album = AlbumDetails(id = "111")
        )

        assertEquals(100_000L, cache.ageAt(now))
        assertTrue(cache.hasAlbum)

        val emptyCache = LAlbumBundleCache.EMPTY
        assertFalse(emptyCache.hasAlbum)
    }

    @Test
    fun `Luscious URL parser helpers`() {
        assertEquals("551361", extractAlbumIdOrNull("/albums/nimbletail-art-comic-strips_551361/"))
        assertEquals("123", extractAlbumIdOrNull("https://members.luscious.net/albums/something_123/"))
        assertNull(extractAlbumIdOrNull("https://members.luscious.net/users/user1/"))
        assertNull(extractAlbumIdOrNull(null))
        assertNull(extractAlbumIdOrNull(""))

        assertTrue(isValidAlbumUrl("/albums/test_999/"))
        assertFalse(isValidAlbumUrl("/users/unknown/"))
        assertFalse(isValidAlbumUrl(null))
    }
}
