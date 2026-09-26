package com.client.xvideos.l.model

import com.client.xvideos.l.model.enum.AudiencesType
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.net.AlbumListImplInfoAndList
import com.client.xvideos.l.net.GetAlbumListAggregationsResult
import com.client.xvideos.l.net.LAlbumBundleCache
import com.client.xvideos.l.net.LAlbumPageLoadIssue
import com.client.xvideos.l.net.LAlbumPicsBundleSnapshot
import com.client.xvideos.l.net.L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS
import com.client.xvideos.l.repository.AlbumResult
import com.client.xvideos.l.repository.RepositoryResult
import com.client.xvideos.l.repository.albumInfoOrNull
import com.client.xvideos.l.repository.errorMessageOrNull
import com.client.xvideos.l.repository.getOrDefault
import com.client.xvideos.l.repository.getOrNull
import com.client.xvideos.l.repository.isError
import com.client.xvideos.l.repository.isLoading
import com.client.xvideos.l.repository.isSuccess
import com.client.xvideos.l.repository.onError
import com.client.xvideos.l.repository.onSuccess
import com.client.xvideos.l.repository.throwableOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Batch55LRepositoryAndCacheTest {

    @Test
    fun `RepositoryResult extension functions and callbacks`() {
        val loading: RepositoryResult = RepositoryResult.Loading
        assertTrue(loading.isLoading)
        assertFalse(loading.isSuccess)
        assertFalse(loading.isError)
        assertNull(loading.getOrNull<String>())
        assertEquals("default", loading.getOrDefault("default"))

        val success: RepositoryResult = RepositoryResult.Success("hello")
        assertFalse(success.isLoading)
        assertTrue(success.isSuccess)
        assertFalse(success.isError)
        assertEquals("hello", success.getOrNull<String>())
        assertEquals("hello", success.getOrDefault("default"))

        var successCallbackCalled = false
        success.onSuccess<String> {
            successCallbackCalled = true
            assertEquals("hello", it)
        }
        assertTrue(successCallbackCalled)

        val errException = IllegalStateException("boom")
        val error: RepositoryResult = RepositoryResult.Error("Network error", errException)
        assertFalse(error.isLoading)
        assertFalse(error.isSuccess)
        assertTrue(error.isError)
        assertEquals("Network error", error.errorMessageOrNull())
        assertEquals(errException, error.throwableOrNull())

        var errorCallbackCalled = false
        error.onError { message, throwable ->
            errorCallbackCalled = true
            assertEquals("Network error", message)
            assertEquals(errException, throwable)
        }
        assertTrue(errorCallbackCalled)
    }

    @Test
    fun `AlbumResult safe extraction`() {
        val emptyResult: AlbumResult = AlbumResult.Empty
        assertNull(emptyResult.albumInfoOrNull)
    }

    @Test
    fun `AudiencesType default fallbacks`() {
        assertEquals(AudiencesType.STRAIGHT, AudiencesType.DEFAULT)
        assertEquals(AudiencesType.GAY, AudiencesType.fromIdOrDefault(2))
        assertEquals(AudiencesType.STRAIGHT, AudiencesType.fromIdOrDefault(999))
        assertEquals(AudiencesType.TRANS, AudiencesType.fromIdOrDefault(null, default = AudiencesType.TRANS))

        assertEquals(AudiencesType.LESBIAN, AudiencesType.fromUrlOrDefault("/audiences/lesbian_3/"))
        assertEquals(AudiencesType.STRAIGHT, AudiencesType.fromUrlOrDefault("/unknown/path/"))
        assertEquals(AudiencesType.SOLO_GIRL, AudiencesType.fromUrlOrDefault(null, default = AudiencesType.SOLO_GIRL))
    }

    @Test
    fun `LAlbumBundleCache inspection and deterministic freshness`() {
        val emptyCache = LAlbumBundleCache.EMPTY
        assertFalse(emptyCache.hasPics)
        assertEquals(0, emptyCache.picsCount)

        val pic = PicsDetails(id = "123")
        val freshCache = LAlbumBundleCache(
            cachedAtMs = 1000L,
            album = AlbumDetails(id = "album_1"),
            pics = listOf(pic)
        )
        assertTrue(freshCache.hasPics)
        assertEquals(1, freshCache.picsCount)
        assertTrue(freshCache.isValid)

        assertTrue(freshCache.isFreshAt(1000L + 1000L))
        assertFalse(freshCache.isFreshAt(1000L + L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS + 1L))
    }

    @Test
    fun `AlbumList models inspection properties`() {
        val emptyList = AlbumListImplInfoAndList()
        assertTrue(emptyList.isEmpty)
        assertFalse(emptyList.isNotEmpty)
        assertEquals(0, emptyList.totalCount)

        val populatedList = AlbumListImplInfoAndList(
            items = listOf(Album(id = "1", title = "Album 1"))
        )
        assertFalse(populatedList.isEmpty)
        assertTrue(populatedList.isNotEmpty)
        assertEquals(1, populatedList.totalCount)

        val aggregations = GetAlbumListAggregationsResult(
            filterGenreStateCount = listOf(AlbumListFilterGenreCountResponse(count = 5, term = "Hentai")),
            filterTaggedStateCount = emptyList(),
            filterPictureCountStateCount = listOf(AlbumListFilterGenreCountResponse(count = 10, term = "C0_25")),
            id = 1,
            filter = null
        )
        assertTrue(aggregations.hasGenres)
        assertFalse(aggregations.hasTags)
        assertTrue(aggregations.hasPictureCounts)
    }

    @Test
    fun `LAlbumPageLoadIssue and LAlbumPicsBundleSnapshot helpers`() {
        val issue = LAlbumPageLoadIssue(page = 1, message = "Timeout", htmlChallenge = false)
        assertTrue(issue.hasMessage)

        val emptySnapshot = LAlbumPicsBundleSnapshot(pics = emptyList(), totalPages = 0)
        assertTrue(emptySnapshot.isEmpty)
        assertFalse(emptySnapshot.isNotEmpty)
        assertEquals(0, emptySnapshot.count)

        val filledSnapshot = LAlbumPicsBundleSnapshot(pics = listOf(PicsDetails(id = "1")), totalPages = 1)
        assertFalse(filledSnapshot.isEmpty)
        assertTrue(filledSnapshot.isNotEmpty)
        assertEquals(1, filledSnapshot.count)
    }
}
