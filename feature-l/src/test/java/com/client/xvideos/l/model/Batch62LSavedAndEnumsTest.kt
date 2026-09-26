package com.client.xvideos.l.model

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.featured.saved.LCollectionEntity
import com.client.xvideos.l.featured.saved.SavedL_Albums
import com.client.xvideos.l.featured.saved.SavedL_Collection
import com.client.xvideos.l.featured.saved.SavedL_Likes
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.enum.PictureCountRank
import com.client.xvideos.l.model.enum.SelectIndex
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class Batch62LSavedAndEnumsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `AlbumType enum flags and parsers`() {
        assertTrue(AlbumType.All.isAll)
        assertFalse(AlbumType.All.isManga)
        assertFalse(AlbumType.All.isPictures)
        assertEquals("All", AlbumType.All.title)

        assertTrue(AlbumType.Manga.isManga)
        assertFalse(AlbumType.Manga.isAll)
        assertEquals("Manga", AlbumType.Manga.title)

        assertTrue(AlbumType.Pictures.isPictures)
        assertFalse(AlbumType.Pictures.isManga)
        assertEquals("Pictures", AlbumType.Pictures.title)

        assertEquals(AlbumType.Pictures, AlbumType.DEFAULT)

        assertEquals(AlbumType.Manga, AlbumType.fromValueOrNull("manga"))
        assertEquals(AlbumType.Pictures, AlbumType.fromValueOrNull("PICTURES"))
        assertEquals(AlbumType.All, AlbumType.fromValueOrNull("all"))
        assertNull(AlbumType.fromValueOrNull(null))
        assertNull(AlbumType.fromValueOrNull("unknown"))

        assertEquals(AlbumType.Pictures, AlbumType.fromValue("nonexistent"))
        assertEquals(AlbumType.Manga, AlbumType.fromValue("manga"))

        assertEquals(AlbumType.Pictures, AlbumType.fromNameOrNull("Pictures"))
        assertEquals(AlbumType.Manga, AlbumType.fromNameOrNull("MANGA"))
        assertNull(AlbumType.fromNameOrNull("unknown"))
        assertNull(AlbumType.fromNameOrNull(null))

        assertEquals(AlbumType.Manga, AlbumType.fromName("Manga"))
        assertEquals(AlbumType.Pictures, AlbumType.fromName("unknown"))

        assertEquals(AlbumType.Manga, AlbumType.fromIdOrDefault("manga"))
        assertEquals(AlbumType.Manga, AlbumType.fromIdOrDefault("Manga"))
        assertEquals(AlbumType.Pictures, AlbumType.fromIdOrDefault(null))
    }

    @Test
    fun `ContentId enum flags and parsers`() {
        assertTrue(ContentId.All.isAll)
        assertFalse(ContentId.All.isHentai)
        assertEquals("All", ContentId.All.title)

        assertTrue(ContentId.Hentai.isHentai)
        assertFalse(ContentId.Hentai.isAll)
        assertEquals("Hentai", ContentId.Hentai.title)

        assertTrue(ContentId.NonErotic.isNonErotic)
        assertEquals("Non-Erotic", ContentId.NonErotic.title)

        assertTrue(ContentId.RealPeople.isRealPeople)
        assertEquals("Real People", ContentId.RealPeople.title)

        assertEquals(ContentId.All, ContentId.DEFAULT)

        assertEquals(ContentId.All, ContentId.fromValueOrNull(0))
        assertEquals(ContentId.Hentai, ContentId.fromValueOrNull(2))
        assertEquals(ContentId.NonErotic, ContentId.fromValueOrNull(5))
        assertEquals(ContentId.RealPeople, ContentId.fromValueOrNull(6))
        assertNull(ContentId.fromValueOrNull(999))
        assertNull(ContentId.fromValueOrNull(null))

        assertEquals(ContentId.Hentai, ContentId.fromValue(2))
        assertEquals(ContentId.All, ContentId.fromValue(999))

        assertEquals(ContentId.Hentai, ContentId.fromNameOrNull("Hentai"))
        assertEquals(ContentId.RealPeople, ContentId.fromNameOrNull("REALPEOPLE"))
        assertNull(ContentId.fromNameOrNull("unknown"))
        assertNull(ContentId.fromNameOrNull(null))

        assertEquals(ContentId.Hentai, ContentId.fromStringOrNull("2"))
        assertEquals(ContentId.Hentai, ContentId.fromStringOrNull("Hentai"))
        assertNull(ContentId.fromStringOrNull("abc"))
        assertNull(ContentId.fromStringOrNull(null))

        assertEquals(ContentId.Hentai, ContentId.fromString("2"))
        assertEquals(ContentId.All, ContentId.fromString("invalid"))

        assertEquals(ContentId.NonErotic, ContentId.fromIdOrDefault("5"))
        assertEquals(ContentId.All, ContentId.fromIdOrDefault(null))
    }

    @Test
    fun `PictureCountRank enum flags and range checks`() {
        assertTrue(PictureCountRank.All.isAll)
        assertFalse(PictureCountRank.All.isSpecific)
        assertEquals("All", PictureCountRank.All.rangeDescription)
        assertTrue(PictureCountRank.All.matchesCount(0))
        assertTrue(PictureCountRank.All.matchesCount(50000))

        assertTrue(PictureCountRank.C0_25.isSpecific)
        assertFalse(PictureCountRank.C0_25.isAll)
        assertEquals("0 - 25", PictureCountRank.C0_25.rangeDescription)
        assertTrue(PictureCountRank.C0_25.matchesCount(0))
        assertTrue(PictureCountRank.C0_25.matchesCount(25))
        assertFalse(PictureCountRank.C0_25.matchesCount(26))

        assertEquals("25 - 50", PictureCountRank.C25_50.rangeDescription)
        assertTrue(PictureCountRank.C25_50.matchesCount(30))
        assertFalse(PictureCountRank.C25_50.matchesCount(10))

        assertEquals("50 - 100", PictureCountRank.C50_100.rangeDescription)
        assertEquals("100 - 200", PictureCountRank.C100_200.rangeDescription)
        assertEquals("200 - 800", PictureCountRank.C200_800.rangeDescription)
        assertEquals("800 - 3200", PictureCountRank.C800_3200.rangeDescription)
        assertEquals("3200 - 12800", PictureCountRank.C3200_12800.rangeDescription)

        assertTrue(PictureCountRank.C3200_12800.matchesCount(5000))
        assertFalse(PictureCountRank.C3200_12800.matchesCount(2000))

        assertEquals(PictureCountRank.All, PictureCountRank.DEFAULT)
        assertEquals(PictureCountRank.C0_25, PictureCountRank.fromCountOrNull(0))
        assertEquals(PictureCountRank.C800_3200, PictureCountRank.fromCountOrNull(5))
        assertNull(PictureCountRank.fromCountOrNull(999))
        assertNull(PictureCountRank.fromCountOrNull(null))

        assertEquals(PictureCountRank.C25_50, PictureCountRank.fromCount(1))
        assertEquals(PictureCountRank.All, PictureCountRank.fromCount(999))

        assertEquals(PictureCountRank.C50_100, PictureCountRank.fromRankOrDefault(2))
        assertEquals(PictureCountRank.All, PictureCountRank.fromRankOrDefault(999))

        assertEquals(PictureCountRank.C0_25, PictureCountRank.fromNameOrNull("C0_25"))
        assertNull(PictureCountRank.fromNameOrNull(null))

        assertEquals(PictureCountRank.C100_200, PictureCountRank.fromStringOrNull("3"))
        assertEquals(PictureCountRank.C100_200, PictureCountRank.fromStringOrNull("C100_200"))
        assertNull(PictureCountRank.fromStringOrNull("unknown"))
        assertNull(PictureCountRank.fromStringOrNull(null))

        assertEquals(PictureCountRank.C200_800, PictureCountRank.fromString("4"))
        assertEquals(PictureCountRank.All, PictureCountRank.fromString("invalid"))
    }

    @Test
    fun `SelectIndex enum flags and parsers`() {
        assertTrue(SelectIndex.Unselect.isUnselect)
        assertFalse(SelectIndex.Unselect.isSelected)
        assertEquals("Unselect", SelectIndex.Unselect.title)

        assertTrue(SelectIndex.Default.isSelected)
        assertTrue(SelectIndex.Default.isDefault)
        assertEquals("Default", SelectIndex.Default.title)

        assertTrue(SelectIndex.Manga.isManga)
        assertEquals("Manga", SelectIndex.Manga.title)

        assertTrue(SelectIndex.Hentai.isHentai)
        assertEquals("Hentai", SelectIndex.Hentai.title)

        assertTrue(SelectIndex.Porn.isPorn)
        assertEquals("Porn", SelectIndex.Porn.title)

        assertEquals(SelectIndex.Default, SelectIndex.DEFAULT)

        assertEquals(SelectIndex.Unselect, SelectIndex.fromValueOrNull(-1))
        assertEquals(SelectIndex.Default, SelectIndex.fromValueOrNull(0))
        assertEquals(SelectIndex.Manga, SelectIndex.fromValueOrNull(1))
        assertEquals(SelectIndex.Hentai, SelectIndex.fromValueOrNull(2))
        assertEquals(SelectIndex.Porn, SelectIndex.fromValueOrNull(3))
        assertNull(SelectIndex.fromValueOrNull(999))
        assertNull(SelectIndex.fromValueOrNull(null))

        assertEquals(SelectIndex.Hentai, SelectIndex.fromValue(2))
        assertEquals(SelectIndex.Default, SelectIndex.fromValue(999))

        assertEquals(SelectIndex.Porn, SelectIndex.fromIndexOrDefault(3))
        assertEquals(SelectIndex.Default, SelectIndex.fromIndexOrDefault(999))

        assertEquals(SelectIndex.Manga, SelectIndex.fromNameOrNull("Manga"))
        assertNull(SelectIndex.fromNameOrNull("unknown"))
        assertNull(SelectIndex.fromNameOrNull(null))

        assertEquals(SelectIndex.Hentai, SelectIndex.fromStringOrNull("2"))
        assertEquals(SelectIndex.Hentai, SelectIndex.fromStringOrNull("Hentai"))
        assertNull(SelectIndex.fromStringOrNull("invalid"))
        assertNull(SelectIndex.fromStringOrNull(null))

        assertEquals(SelectIndex.Default, SelectIndex.fromString("unknown"))
        assertEquals(SelectIndex.Manga, SelectIndex.fromString("1"))
    }

    @Test
    fun `LCollectionEntity helper properties`() {
        val entity1 = LCollectionEntity(
            collection = "Cosplay Favs",
            previewUrl = "file:///sdcard/cover.jpg",
            itemsCount = 15,
            lastModifiedAt = 123456789L,
            duplicateCount = 2,
            hasManualCover = true
        )
        assertEquals("Cosplay Favs", entity1.name)
        assertFalse(entity1.isEmpty)
        assertTrue(entity1.isNotEmpty)
        assertTrue(entity1.hasDuplicates)
        assertTrue(entity1.hasPreview)

        val entity2 = LCollectionEntity(
            collection = "Empty Collection",
            previewUrl = null,
            itemsCount = 0,
            lastModifiedAt = 123456789L,
            duplicateCount = 0,
            hasManualCover = false
        )
        assertEquals("Empty Collection", entity2.name)
        assertTrue(entity2.isEmpty)
        assertFalse(entity2.isNotEmpty)
        assertFalse(entity2.hasDuplicates)
        assertFalse(entity2.hasPreview)
    }

    @Test
    fun `SavedL_Likes, SavedL_Albums and SavedL_Collection helper queries`() = runTest {
        val tempDir = Files.createTempDirectory("app_path_test_batch62").toFile()
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)

        val fileDb = AppFileDatabase()
        val repository = Repository(fileDb)
        val luscious = Luscious(this, repository)

        val savedLikes = SavedL_Likes(luscious, this)
        val savedAlbums = SavedL_Albums(fileDb, this)
        val savedCollection = SavedL_Collection(this, luscious)

        // SavedL_Likes checks
        assertTrue(savedLikes.isEmpty)
        assertFalse(savedLikes.isNotEmpty)
        assertEquals(0, savedLikes.count)
        assertFalse(savedLikes.contains("item-1"))
        assertFalse(savedLikes.contains(null as String?))
        assertNull(savedLikes.findByIdOrNull("item-1"))
        assertNull(savedLikes.findByIdOrNull(null))

        val picItem = PicsDetails(id = "item-1", album = "album-1")
        savedLikes.listUrl.add(picItem)
        assertFalse(savedLikes.isEmpty)
        assertTrue(savedLikes.isNotEmpty)
        assertEquals(1, savedLikes.count)
        assertTrue(savedLikes.contains("item-1"))
        assertTrue(savedLikes.contains(picItem))
        assertNotNull(savedLikes.findByIdOrNull("item-1"))
        assertEquals("album-1", savedLikes.findByIdOrNull("item-1")?.album)

        // SavedL_Albums checks
        assertTrue(savedAlbums.isEmpty)
        assertFalse(savedAlbums.isNotEmpty)
        assertEquals(0, savedAlbums.count)
        assertFalse(savedAlbums.contains("album-1"))
        assertFalse(savedAlbums.contains(null as String?))
        assertNull(savedAlbums.findByIdOrNull("album-1"))
        assertNull(savedAlbums.findByIdOrNull(null))

        val albumItem = AlbumDetails(id = "album-1", title = "Cool Album")
        savedAlbums.list.add(albumItem)
        assertFalse(savedAlbums.isEmpty)
        assertTrue(savedAlbums.isNotEmpty)
        assertEquals(1, savedAlbums.count)
        assertTrue(savedAlbums.contains("album-1"))
        assertTrue(savedAlbums.contains(albumItem))
        assertNotNull(savedAlbums.findByIdOrNull("album-1"))
        assertEquals("Cool Album", savedAlbums.findByIdOrNull("album-1")?.title)

        // SavedL_Collection checks
        assertTrue(savedCollection.isCollectionsEmpty)
        assertFalse(savedCollection.isCollectionsNotEmpty)
        assertEquals(0, savedCollection.collectionsCount)
        assertTrue(savedCollection.isCurrentItemsEmpty)
        assertFalse(savedCollection.isCurrentItemsNotEmpty)
        assertEquals(0, savedCollection.currentItemsCount)
        assertFalse(savedCollection.containsCollection("FanArt"))
        assertFalse(savedCollection.containsCollection(null))
        assertNull(savedCollection.findCollectionByNameOrNull("FanArt"))
        assertNull(savedCollection.findCollectionByNameOrNull(null))

        val collectionEntity = LCollectionEntity(
            collection = "FanArt",
            previewUrl = null,
            itemsCount = 5,
            lastModifiedAt = 1000L,
            duplicateCount = 0,
            hasManualCover = false
        )
        savedCollection.collectionList.add(collectionEntity)
        assertFalse(savedCollection.isCollectionsEmpty)
        assertTrue(savedCollection.isCollectionsNotEmpty)
        assertEquals(1, savedCollection.collectionsCount)
        assertTrue(savedCollection.containsCollection("fanart"))
        assertTrue(savedCollection.containsCollection("FanArt"))
        assertNotNull(savedCollection.findCollectionByNameOrNull("fanart"))
        assertEquals("FanArt", savedCollection.findCollectionByNameOrNull("FanArt")?.collection)

        savedCollection.listUrl.add(picItem)
        assertFalse(savedCollection.isCurrentItemsEmpty)
        assertTrue(savedCollection.isCurrentItemsNotEmpty)
        assertEquals(1, savedCollection.currentItemsCount)
    }
}
