package com.client.xvideos.n

import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.ui.screens.explorer.N_ScreenExplorer
import com.client.xvideos.n.ui.screens.explorer.tab.list.N_ScreenGalleryList
import com.client.xvideos.n.ui.screens.explorer.tab.saved.N_SavedTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.N_ScreenSavedCollectionsTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.likes.N_ScreenSavedLikesTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.N_ScreenSavedMangaTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.N_ScreenServerFavoritesTab
import com.client.xvideos.n.ui.screens.explorer.tab.search.N_ScreenGallerySearch
import com.client.xvideos.n.ui.screens.explorer.tab.tophits.N_ScreenGalleryTopHits
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery
import com.client.xvideos.n.ui.screens.picture.N_FullScreenPicture
import com.client.xvideos.n.ui.screens.reader.N_FullScreenReader
import com.client.xvideos.n.ui.screens.tag.ScreenNTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Тесты сериализации экранов Voyager раздела nhentai для сохранения состояния навигации.
 */
class NScreenSerializationTest {

    private inline fun <reified T : Serializable> assertJavaSerialization(screen: T): T {
        val byteOut = ByteArrayOutputStream()
        ObjectOutputStream(byteOut).use { it.writeObject(screen) }

        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        val restored = ObjectInputStream(byteIn).use { it.readObject() }

        assertNotNull(restored)
        return restored as T
    }

    @Test
    fun `N_ScreenExplorer сохраняет ключ и сериализуется`() {
        val restored = assertJavaSerialization(N_ScreenExplorer())
        assertEquals("N_ScreenExplorer", restored.key)
    }

    @Test
    fun `Вкладки проводника nhentai успешно сериализуются`() {
        assertEquals("N_ScreenGalleryList", assertJavaSerialization(N_ScreenGalleryList).key)
        assertEquals("N_ScreenGalleryTopHits", assertJavaSerialization(N_ScreenGalleryTopHits).key)
        assertEquals("N_ScreenGallerySearch", assertJavaSerialization(N_ScreenGallerySearch).key)
        assertEquals("N_SavedTab", assertJavaSerialization(N_SavedTab).key)
    }

    @Test
    fun `Вкладки сохраненного раздела nhentai успешно сериализуются`() {
        assertEquals("N_ScreenSavedCollectionsTab", assertJavaSerialization(N_ScreenSavedCollectionsTab).key)
        assertEquals("N_ScreenSavedLikesTab", assertJavaSerialization(N_ScreenSavedLikesTab).key)
        assertEquals("N_ScreenSavedMangaTab", assertJavaSerialization(N_ScreenSavedMangaTab).key)
        assertEquals("N_ScreenServerFavoritesTab", assertJavaSerialization(N_ScreenServerFavoritesTab).key)
    }

    @Test
    fun `ScreenNGallery сохраняет детерминированный ключ и восстанавливается`() {
        val screen = ScreenNGallery(galleryId = 697416L)
        val restored = assertJavaSerialization(screen)
        assertEquals("NGallery:697416", restored.key)
        assertEquals(697416L, restored.galleryId)
    }

    @Test
    fun `ScreenNTag сохраняет метаданные тега и восстанавливается`() {
        val tag = NTag(id = 123503L, type = "parody", name = "genshin impact", count = 4518)
        val screen = ScreenNTag(tag = tag)
        val restored = assertJavaSerialization(screen)
        assertEquals("NTag:parody:123503:genshin impact", restored.key)
        assertEquals("genshin impact", restored.tag.name)
        assertEquals("parody", restored.tag.type)
    }

    @Test
    fun `N_FullScreenReader успешно сериализуется`() {
        val gallery = NGallery(id = 100L, mediaId = 200L, title = "Reader Title")
        val screen = N_FullScreenReader(gallery = gallery, initialPage = 2)
        val restored = assertJavaSerialization(screen)
        assertEquals("NReader:100:2", restored.key)
        assertEquals(2, restored.initialPage)
    }

    @Test
    fun `N_FullScreenPicture успешно сериализуется`() {
        val picture = NPicture(id = "100_2", galleryId = 100L, mediaId = 200L, pageNumber = 2)
        val screen = N_FullScreenPicture(pictures = arrayListOf(picture), initialIndex = 0)
        val restored = assertJavaSerialization(screen)
        assertEquals("NPicture:100_2:0", restored.key)
        assertEquals(1, restored.pictures.size)
        assertEquals("100_2", restored.pictures.first().id)
    }
}
