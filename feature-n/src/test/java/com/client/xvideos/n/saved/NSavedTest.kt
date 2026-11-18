package com.client.xvideos.n.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.screens.explorer.tab.saved.nSavedTabPage
import com.client.xvideos.n.ui.screens.explorer.tab.saved.nSavedTabPageCount
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
        collectionRepo = NCollectionRepository()
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
    fun `collection repository creates collections and manages items`() {
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
    fun `collection repository sets collection cover without changing items order`() {
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
    fun `NSaved refreshAll executes without errors`() {
        assertNotNull(saved.favorites)
        assertNotNull(saved.likes)
        assertNotNull(saved.collection)
        saved.refreshAll()
    }

    @Test
    fun `favorites repository imports items from server batch`() {
        val gallery1 = NGalleryPreview(id = 101L, mediaId = 201L, title = "Manga 1", thumbnailUrl = "thumb1", numPages = 20)
        val gallery2 = NGalleryPreview(id = 102L, mediaId = 202L, title = "Manga 2", thumbnailUrl = "thumb2", numPages = 25)

        assertFalse(favoritesRepo.isFavorite(101L))
        assertFalse(favoritesRepo.isFavorite(102L))

        val importedCount = favoritesRepo.importFavorites(listOf(gallery1, gallery2))
        assertEquals(2, importedCount)
        assertTrue(favoritesRepo.isFavorite(101L))
        assertTrue(favoritesRepo.isFavorite(102L))

        // Повторный импорт не дублирует
        val secondImport = favoritesRepo.importFavorites(listOf(gallery1))
        assertEquals(0, secondImport)
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
