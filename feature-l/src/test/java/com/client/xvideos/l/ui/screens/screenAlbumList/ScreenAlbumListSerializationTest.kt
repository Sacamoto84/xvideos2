package com.client.xvideos.l.ui.screens.screenAlbumList

import com.client.xvideos.l.ui.screens.albumLandingTag.ScreenLAlbumLandingTag
import com.client.xvideos.l.ui.screens.explorer.L_ScreenExplorer
import com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.L_ScreenAlbumSearch
import com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits.L_ScreenAlbumTopHits
import com.client.xvideos.l.ui.screens.explorer.tab.saved.L_SavedTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.albums.L_ScreenSavedAlbumsTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.L_Screen_CollectionTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.L_ScreenSavedLikesTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes.L_ScreenServerLikesTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.L_ScreenSubscribedAlbumsTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class ScreenAlbumListSerializationTest {

    private inline fun <reified T : Serializable> assertJavaSerialization(screen: T): T {
        val byteOut = ByteArrayOutputStream()
        ObjectOutputStream(byteOut).use { it.writeObject(screen) }

        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        val restored = ObjectInputStream(byteIn).use { it.readObject() }

        assertNotNull(restored)
        return restored as T
    }

    @Test
    fun `L_ScreenAlbumList preserves deterministic key and survives serialization`() {
        val restored = assertJavaSerialization(L_ScreenAlbumList)
        assertEquals("L_ScreenAlbumList", restored.key)
    }

    @Test
    fun `ScreenLAlbumList created via factory preserves deterministic key and survives serialization`() {
        val screen = L_ScreenAlbumList.create(filter = null, title = "Trending Albums")
        val restored = assertJavaSerialization(screen)
        assertTrue(restored.key.startsWith("ScreenLAlbumList:0:Trending Albums"))
    }

    @Test
    fun `ScreenLAlbumLandingTag preserves deterministic key and survives serialization`() {
        val screen = ScreenLAlbumLandingTag("cosplay")
        val restored = assertJavaSerialization(screen)
        assertEquals("ScreenLAlbumLandingTag:cosplay", restored.key)
    }

    @Test
    fun `feature-l singleton screens preserve deterministic keys and survive serialization`() {
        assertEquals("L_ScreenExplorer", assertJavaSerialization(L_ScreenExplorer()).key)
        assertEquals("L_SavedTab", assertJavaSerialization(L_SavedTab).key)
        assertEquals("L_ScreenAlbumSearch", assertJavaSerialization(L_ScreenAlbumSearch).key)
        assertEquals("L_ScreenAlbumTopHits", assertJavaSerialization(L_ScreenAlbumTopHits).key)
        assertEquals("L_ScreenSavedAlbumsTab", assertJavaSerialization(L_ScreenSavedAlbumsTab).key)
        assertEquals("L_ScreenSavedLikesTab", assertJavaSerialization(L_ScreenSavedLikesTab).key)
        assertEquals("L_Screen_CollectionTab", assertJavaSerialization(L_Screen_CollectionTab).key)
        assertEquals("L_ScreenServerLikesTab", assertJavaSerialization(L_ScreenServerLikesTab).key)
        assertEquals("L_ScreenSubscribedAlbumsTab", assertJavaSerialization(L_ScreenSubscribedAlbumsTab).key)
    }
}
