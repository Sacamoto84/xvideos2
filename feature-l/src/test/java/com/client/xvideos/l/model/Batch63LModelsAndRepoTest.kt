package com.client.xvideos.l.model

import com.client.xvideos.l.featured.saved.LAlbumExporter
import com.client.xvideos.l.featured.saved.LSavedLikeMetadata
import com.client.xvideos.l.featured.saved.LSavedLikePreview
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.AudiencesType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import com.client.xvideos.l.net.AlbumInfo
import com.client.xvideos.l.repository.AlbumResult
import com.client.xvideos.l.repository.RepositoryResult
import com.client.xvideos.l.repository.albumInfoOrNull
import com.client.xvideos.l.repository.hasAlbumInfo
import com.client.xvideos.l.repository.isEmpty
import com.client.xvideos.l.repository.isNotEmpty
import com.client.xvideos.l.repository.map
import com.client.xvideos.l.repository.onLoading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class Batch63LModelsAndRepoTest {

    @Test
    fun `AudiencesType convenience flags and lookup helpers`() {
        assertTrue(AudiencesType.SOLO_GIRL.isSolo)
        assertTrue(AudiencesType.SOLO_GUY.isSolo)
        assertFalse(AudiencesType.STRAIGHT.isSolo)

        assertTrue(AudiencesType.TRANS.isTrans)
        assertTrue(AudiencesType.TRANS_X_GIRL.isTrans)
        assertTrue(AudiencesType.TRANS_X_GUY.isTrans)
        assertTrue(AudiencesType.TRANS_X_TRANS.isTrans)
        assertFalse(AudiencesType.GAY.isTrans)

        assertTrue(AudiencesType.GAY.matchesTitle("yaoi"))
        assertTrue(AudiencesType.LESBIAN.matchesTitle("YURI"))
        assertFalse(AudiencesType.STRAIGHT.matchesTitle(null))
        assertFalse(AudiencesType.STRAIGHT.matchesTitle(""))

        assertEquals(AudiencesType.SOLO_GIRL, AudiencesType.fromTitleOrNull("Solo Girl"))
        assertEquals(AudiencesType.SOLO_GIRL, AudiencesType.fromTitleOrNull("solo girl"))
        assertNull(AudiencesType.fromTitleOrNull(null))
        assertNull(AudiencesType.fromTitleOrNull("nonexistent"))

        assertEquals(AudiencesType.STRAIGHT, AudiencesType.fromTitleOrDefault("unknown"))
        assertEquals(AudiencesType.LESBIAN, AudiencesType.fromTitleOrDefault("Lesbian / Yuri"))

        assertEquals(AudiencesType.STRAIGHT, AudiencesType.fromStringOrNull("1"))
        assertEquals(AudiencesType.GAY, AudiencesType.fromStringOrNull("Gay / Yaoi"))
        assertEquals(AudiencesType.LESBIAN, AudiencesType.fromStringOrNull("/audiences/lesbian_3/"))
        assertNull(AudiencesType.fromStringOrNull(null))
        assertNull(AudiencesType.fromStringOrNull("invalid"))
    }

    @Test
    fun `Display model helpers and lookups`() {
        val displayItem = DataAlbumFilterDisplay(primary = "By Date", secondary = "Newest First", request = "date_newest")
        assertTrue(displayItem.isValid)
        assertTrue(displayItem.isByDate)
        assertFalse(displayItem.isByTopRated)
        assertFalse(displayItem.isByFirstLetter)
        assertEquals("By Date: Newest First", displayItem.displayTitle)
        assertTrue(displayItem.matchesRequest("date_newest"))
        assertTrue(displayItem.matchesRequest("DATE_NEWEST"))
        assertFalse(displayItem.matchesRequest("other"))
        assertFalse(displayItem.matchesRequest(null))

        val singleDisplay = DataAlbumFilterDisplay(primary = "First Letter", secondary = "", request = "alpha_any")
        assertEquals("First Letter", singleDisplay.displayTitle)

        assertNull(findAlbumFilterDisplayByRequest(null))
        assertNull(findAlbumFilterDisplayByRequest(""))
        assertEquals("rating_7_days", findAlbumFilterDisplayByRequest("rating_7_days")?.request)

        val byPrimarySec = findAlbumFilterDisplayByPrimaryAndSecondary("By Top Rated", "7 Days")
        assertNotNull(byPrimarySec)
        assertEquals("rating_7_days", byPrimarySec?.request)
        assertNull(findAlbumFilterDisplayByPrimaryAndSecondary(null, "7 Days"))
        assertNull(findAlbumFilterDisplayByPrimaryAndSecondary("By Top Rated", null))
        assertNull(findAlbumFilterDisplayByPrimaryAndSecondary("Invalid", "Invalid"))

        val topRatedList = getAlbumFilterDisplaysByPrimary("By Top Rated")
        assertEquals(6, topRatedList.size)
        assertTrue(getAlbumFilterDisplaysByPrimary(null).isEmpty())
        assertTrue(getAlbumFilterDisplaysByPrimary("Unknown").isEmpty())
    }

    @Test
    fun `AlbumListFilter predicates and modifications`() {
        val defaultFilter = AlbumListFilter.DEFAULT
        assertTrue(defaultFilter.isDefault)
        assertFalse(defaultFilter.hasPictureRankFilter)
        assertFalse(defaultFilter.hasContentIdFilter)
        assertFalse(defaultFilter.hasAlbumTypeFilter)

        val modifiedFilter = defaultFilter.copy(
            picture_count_rank = PictureCountRank.C0_25,
            content_id = ContentId.Hentai,
            album_type = AlbumType.Manga,
            tagPlus = listOf("Cosplay"),
            tagMinus = listOf("Gore"),
            genresPlus = listOf(FilterGenre(title = "Action")),
            genresMinus = listOf(FilterGenre(title = "Horror"))
        )

        assertFalse(modifiedFilter.isDefault)
        assertTrue(modifiedFilter.hasPictureRankFilter)
        assertTrue(modifiedFilter.hasContentIdFilter)
        assertTrue(modifiedFilter.hasAlbumTypeFilter)

        assertTrue(modifiedFilter.containsTagPlus("cosplay"))
        assertTrue(modifiedFilter.containsTagPlus("COSPLAY"))
        assertFalse(modifiedFilter.containsTagPlus("Fantasy"))
        assertFalse(modifiedFilter.containsTagPlus(null))

        assertTrue(modifiedFilter.containsTagMinus("gore"))
        assertFalse(modifiedFilter.containsTagMinus("Action"))
        assertFalse(modifiedFilter.containsTagMinus(null))

        assertTrue(modifiedFilter.containsGenrePlus("action"))
        assertFalse(modifiedFilter.containsGenrePlus("Romance"))
        assertFalse(modifiedFilter.containsGenrePlus(null))

        assertTrue(modifiedFilter.containsGenreMinus("horror"))
        assertFalse(modifiedFilter.containsGenreMinus("Comedy"))
        assertFalse(modifiedFilter.containsGenreMinus(null))

        val withQuery = modifiedFilter.withSearchQuery("test query")
        assertEquals("test query", withQuery.searchQuery)

        val withDisp = modifiedFilter.withDisplay("rating_all_time")
        assertEquals("rating_all_time", withDisp.display)
    }

    @Test
    fun `LSavedLikePreview and LSavedLikeMetadata helper properties`() {
        val preview = LSavedLikePreview(
            fileName = "thumb.jpg",
            sourceUrl = "https://cdn/thumb.jpg",
            width = 300,
            height = 200,
            size = "small"
        )
        assertTrue(preview.hasSize)
        assertEquals(60000, preview.area)
        assertTrue(preview.hasValidDimensions)

        val emptyPreview = LSavedLikePreview("t.jpg", "https://", 0, 0, null)
        assertFalse(emptyPreview.hasSize)
        assertEquals(0, emptyPreview.area)
        assertFalse(emptyPreview.hasValidDimensions)

        val meta = LSavedLikeMetadata(
            albumId = "123",
            pictureId = "pic_456",
            mediaFileName = "media.jpg",
            sourceVideoUrl = "https://cdn/vid.mp4",
            previewFiles = listOf(preview)
        )
        assertTrue(meta.hasAlbum)
        assertTrue(meta.hasPictureId)
        assertTrue(meta.hasMediaFile)
        assertTrue(meta.isAnimated)
        assertEquals(1, meta.previewsCount)

        val emptyMeta = LSavedLikeMetadata()
        assertFalse(emptyMeta.hasAlbum)
        assertFalse(emptyMeta.hasPictureId)
        assertFalse(emptyMeta.hasMediaFile)
        assertFalse(emptyMeta.isAnimated)
        assertEquals(0, emptyMeta.previewsCount)
    }

    @Test
    fun `LAlbumExporter helpers`() {
        assertEquals("12345.album", LAlbumExporter.getAlbumFileName("12345"))
        val album = AlbumDetails(id = "67890", title = "Test Album")
        assertEquals("67890.album", LAlbumExporter.getAlbumFileName(album))

        val tempDir = Files.createTempDirectory("l_album_exporter_test").toFile()
        assertFalse(LAlbumExporter.isAlbumSaved("67890", tempDir))
        assertFalse(LAlbumExporter.isAlbumSaved("invalid", tempDir))

        val albumFile = File(tempDir, "67890.album")
        albumFile.writeText("data")
        assertTrue(LAlbumExporter.isAlbumSaved("67890", tempDir))
    }

    @Test
    fun `AlbumResult extensions`() = kotlinx.coroutines.test.runTest {
        val empty: AlbumResult = AlbumResult.Empty
        assertTrue(empty.isEmpty)
        assertFalse(empty.isNotEmpty)
        assertFalse(empty.hasAlbumInfo)
        assertNull(empty.albumInfoOrNull)

        val fileDb = com.client.xvideos.common.fileDB.folder.AppFileDatabase()
        val repository = com.client.xvideos.l.repository.Repository(fileDb)
        val albumInfo = AlbumInfo(12345, repository, this)
        val success: AlbumResult = AlbumResult.Albums(albumInfo)
        assertFalse(success.isEmpty)
        assertTrue(success.isNotEmpty)
        assertTrue(success.hasAlbumInfo)
        assertNotNull(success.albumInfoOrNull)
        assertEquals(12345, success.albumInfoOrNull?.id)
    }

    @Test
    fun `RepositoryResult onLoading and map operators`() {
        var loadingCalled = false
        val loading: RepositoryResult = RepositoryResult.Loading
        loading.onLoading { loadingCalled = true }
        assertTrue(loadingCalled)

        var successLoadingCalled = false
        val success: RepositoryResult = RepositoryResult.Success("hello")
        success.onLoading { successLoadingCalled = true }
        assertFalse(successLoadingCalled)

        val mapped = success.map<String, Int> { it.length }
        assertTrue(mapped is RepositoryResult.Success<*>)
        assertEquals(5, (mapped as RepositoryResult.Success<*>).data)

        val error: RepositoryResult = RepositoryResult.Error("error")
        val mappedError = error.map<String, Int> { it.length }
        assertTrue(mappedError is RepositoryResult.Error)
        assertEquals("error", (mappedError as RepositoryResult.Error).message)

        val mappedLoading = loading.map<String, Int> { it.length }
        assertTrue(mappedLoading is RepositoryResult.Loading)
    }
}
