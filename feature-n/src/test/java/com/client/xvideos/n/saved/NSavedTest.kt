package com.client.xvideos.n.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.collectionDB.CollectionDB
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.screens.explorer.tab.saved.nSavedTabPage
import com.client.xvideos.n.ui.screens.explorer.tab.saved.nSavedTabPageCount
import com.client.xvideos.n.ui.screens.picture.N_FullScreenPictureSM
import com.client.xvideos.n.ui.screens.picture.savedPictureIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NSavedTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var likesRepo: NLikesRepository
    private lateinit var collectionRepo: NCollectionRepository
    private lateinit var favoritesRepo: NFavoritesRepository
    private lateinit var saved: NSaved

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("n_saved_test")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)

        likesRepo = NLikesRepository()
        collectionRepo = NCollectionRepository(CoroutineScope(Job().apply { cancel() }))
        favoritesRepo = NFavoritesRepository()
        saved = NSaved(favoritesRepo, likesRepo, collectionRepo)
    }

    @Test
    fun `likes repository saves and toggles likes`() {
        val pic = NPicture(
            id = NPicture.makeId(12345L, 3),
            galleryId = 12345L,
            mediaId = 67890L,
            pageNumber = 3,
            galleryTitle = "Test Gallery"
        )

        assertFalse(likesRepo.isLiked(12345L, 3))

        val added = likesRepo.toggleLike(pic)
        assertTrue(added)
        assertTrue(likesRepo.isLiked(12345L, 3))
        assertEquals(1, likesRepo.likes.size)

        val removed = likesRepo.toggleLike(pic)
        assertFalse(removed)
        assertFalse(likesRepo.isLiked(12345L, 3))
        assertEquals(0, likesRepo.likes.size)
    }

    @Test
    fun `collection repository creates collections and manages items`() = runTest {
        val created = collectionRepo.createCollection("ArtCollection")
        assertTrue(created)
        assertTrue(collectionRepo.collections.any { it.collection == "ArtCollection" })

        val pic = NPicture.fromPage(
            galleryId = 999L,
            mediaId = 888L,
            galleryTitle = "Test Title",
            page = NPage(pageNumber = 1, ext = "png")
        )

        val itemAdded = collectionRepo.addToCollection("ArtCollection", pic)
        assertTrue(itemAdded)

        collectionRepo.openCollection("ArtCollection")
        assertEquals("ArtCollection", collectionRepo.currentCollectionName)
        assertEquals(1, collectionRepo.currentCollectionItems.size)
        assertEquals(pic.id, collectionRepo.currentCollectionItems.first().id)

        val itemRemoved = collectionRepo.removeFromCollection("ArtCollection", pic.id)
        assertTrue(itemRemoved)
        assertEquals(0, collectionRepo.currentCollectionItems.size)

        val renamed = collectionRepo.renameCollection("ArtCollection", "RenamedArt")
        assertTrue(renamed)
        assertEquals("RenamedArt", collectionRepo.currentCollectionName)

        val deleted = collectionRepo.deleteCollection("RenamedArt")
        assertTrue(deleted)
        assertNull(collectionRepo.currentCollectionName)
        assertFalse(collectionRepo.collections.any { it.collection == "RenamedArt" })
    }

    @Test
    fun `collection repository sets collection cover without changing items order`() = runTest {
        val pic1 = NPicture(id = "1_1", galleryId = 1L, mediaId = 1L, pageNumber = 1)
        val pic2 = NPicture(id = "1_2", galleryId = 1L, mediaId = 1L, pageNumber = 2)

        collectionRepo.createCollection("CoversTest")
        collectionRepo.addToCollection("CoversTest", pic1)
        collectionRepo.addToCollection("CoversTest", pic2)

        val updated = collectionRepo.setCollectionCover("CoversTest", pic2)
        assertTrue(updated)

        collectionRepo.openCollection("CoversTest")
        assertEquals(2, collectionRepo.currentCollectionItems.size)
        assertEquals("1_2", collectionRepo.getCollectionConfig("CoversTest").coverPictureId)
        val coverUrl = collectionRepo.getCollectionCoverUrl("CoversTest", collectionRepo.currentCollectionItems)
        assertEquals(pic2.thumbnailUrl, coverUrl)

        val renamed = collectionRepo.renameCollection("CoversTest", "CoversRenamed")
        assertTrue(renamed)
        assertEquals("1_2", collectionRepo.getCollectionConfig("CoversRenamed").coverPictureId)

        collectionRepo.removeFromCollection("CoversRenamed", pic2.id)
        assertNull(collectionRepo.getCollectionConfig("CoversRenamed").coverPictureId)
    }

    @Test
    fun `NSaved refreshAll executes without errors`() = runTest {
        assertNotNull(saved.favorites)
        assertNotNull(saved.likes)
        assertNotNull(saved.collection)
        saved.refreshAll()
    }

    @Test
    fun `editing one collection leaves unrelated cached collections alone`() = runTest {
        collectionRepo.createCollection("Second")
        val otherPicture = NPicture(id = "2_1")
        val db = CollectionDB(AppPath.n_collection, NPicture.serializer())
        assertTrue(db.insert(otherPicture.id, "Second", otherPicture).isSuccess)

        val added = NPicture(id = "1_1")
        assertTrue(collectionRepo.createCollectionAndAdd("First", added))
        assertEquals(listOf(added), collectionRepo.collections.first { it.collection == "First" }.items)
        assertTrue(collectionRepo.collections.first { it.collection == "Second" }.items.isEmpty())
        collectionRepo.refresh()
        assertEquals(listOf(otherPicture), collectionRepo.collections.first { it.collection == "Second" }.items)
    }

    @Test
    fun `favorite deletion failure is reported to the caller`() {
        val gallery = NGallery(id = 411)
        val blockedPath = File(AppPath.n_favorites, "411.manga")
        assertTrue(blockedPath.mkdir())
        File(blockedPath, "entry").writeText("test")

        assertTrue(favoritesRepo.toggleFavorite(gallery).isFailure)
        assertTrue(favoritesRepo.removeFavorite(gallery.id).isFailure)
        assertTrue(blockedPath.exists())
    }

    @Test
    fun `return position follows the picture id through reordering or deletion`() {
        val pictures = listOf(NPicture(id = "1_3"), NPicture(id = "1_1"), NPicture(id = "1_5"))

        assertEquals(0, savedPictureIndex(pictures, "1_3", previousIndex = 2))
        assertEquals(2, savedPictureIndex(pictures, "deleted", previousIndex = 8))
        assertNull(savedPictureIndex(emptyList(), "deleted", previousIndex = 8))
    }

    @Test
    fun `favorites repository imports items from server batch`() {
        val gallery1 = NGalleryPreview(id = 101L, mediaId = 201L, title = "Manga 1", thumbnailUrl = "thumb1", numPages = 20)
        val gallery2 = NGalleryPreview(id = 102L, mediaId = 202L, title = "Manga 2", thumbnailUrl = "thumb2", numPages = 25)

        assertFalse(favoritesRepo.isFavorite(101L))
        assertFalse(favoritesRepo.isFavorite(102L))

        val imported = favoritesRepo.importFavorites(listOf(gallery1, gallery2))
        assertEquals(NFavoritesImport(added = 2, skipped = 0, failed = 0), imported)
        assertTrue(favoritesRepo.isFavorite(101L))
        assertTrue(favoritesRepo.isFavorite(102L))

        // Повторный импорт не дублирует
        val secondImport = favoritesRepo.importFavorites(listOf(gallery1))
        assertEquals(NFavoritesImport(added = 0, skipped = 1, failed = 0), secondImport)
    }

    @Test
    fun `new collection with a duplicate name leaves the existing picture untouched`() = runTest {
        val picture = NPicture(id = "1_1", galleryTitle = "First title")
        assertTrue(collectionRepo.createCollectionAndAdd("Art", picture))
        assertFalse(collectionRepo.createCollection(" Art "))
        assertFalse(collectionRepo.createCollectionAndAdd("Art", picture.copy(galleryTitle = "New title")))
        assertEquals(listOf(picture), collectionRepo.readPictures("Art"))
        assertFalse(collectionRepo.addToCollection("Missing", picture))
        assertFalse(File(AppPath.n_collection, "Missing").exists())
    }

    @Test
    fun `restored viewer starts from saved picture id`() = runTest {
        val initial = NPicture(id = "1_1")
        val selected = NPicture(id = "1_2")
        assertTrue(collectionRepo.createCollectionAndAdd("Saved", initial))
        assertTrue(collectionRepo.addToCollection("Saved", selected))

        val pages = N_FullScreenPictureSM(saved).loadPictures("Saved", initial, selected.id)
        assertEquals(selected, pages.pictures[pages.position.page])
    }

    @Test
    fun `favorites import reports failed writes separately from duplicates`() {
        val favoritesPath = File(AppPath.n_favorites)
        assertTrue(favoritesPath.delete())
        favoritesPath.writeText("unavailable directory")
        val gallery = NGalleryPreview(id = 101L)

        assertTrue(favoritesRepo.importFavorite(gallery).isFailure)
        assertEquals(NFavoritesImport(added = 0, skipped = 0, failed = 1), favoritesRepo.importFavorites(listOf(gallery)))
    }

    @Test
    fun `nSavedTabPageCount and nSavedTabPage behave correctly`() {
        assertEquals(3, nSavedTabPageCount(isLoggedIn = false))
        assertEquals(4, nSavedTabPageCount(isLoggedIn = true))

        assertEquals(0, nSavedTabPage(0, 3))
        assertEquals(2, nSavedTabPage(2, 3))
        assertEquals(2, nSavedTabPage(3, 3)) // ограничение при выходе из аккаунта
        assertEquals(3, nSavedTabPage(3, 4))
    }
}
