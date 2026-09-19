package com.client.xvideos.r.ui.profile

import com.client.xvideos.r.ui.explorer.ScreenRedExplorer
import com.client.xvideos.r.ui.explorer.tab.FavoritesTab
import com.client.xvideos.r.ui.explorer.tab.gifs.R_ScreenGifsTab
import com.client.xvideos.r.ui.explorer.tab.niches.R_ScreenNichesTab
import com.client.xvideos.r.ui.explorer.tab.saved.R_ScreenSavedTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CollectionTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CreatorsTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_DownloadTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_LikesTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_SubscriptionsTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.SavedNichesTab
import com.client.xvideos.r.ui.manager_block.ScreenRedManageBlock
import com.client.xvideos.r.ui.niche.R_ScreenNiche
import com.client.xvideos.r.ui.root.R_Screen_Root
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class ScreenRedProfileSerializationTest {

    private inline fun <reified T : Serializable> assertJavaSerialization(screen: T): T {
        val byteOut = ByteArrayOutputStream()
        ObjectOutputStream(byteOut).use { it.writeObject(screen) }

        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        val restored = ObjectInputStream(byteIn).use { it.readObject() }

        assertNotNull(restored)
        return restored as T
    }

    @Test
    fun `ScreenRedProfile survives Java serialization and preserves deterministic key`() {
        val screen = ScreenRedProfile("test_user_42")
        val restored = assertJavaSerialization(screen)
        assertEquals("test_user_42", restored.profileName)
        assertEquals("RedProfile:test_user_42", restored.key)
    }

    @Test
    fun `R_ScreenNiche survives Java serialization and preserves deterministic key`() {
        val screen = R_ScreenNiche("cosplay")
        val restored = assertJavaSerialization(screen)
        assertEquals("cosplay", restored.nicheName)
        assertEquals("RedNiche:cosplay", restored.key)
    }

    @Test
    fun `singleton screens in feature-r preserve deterministic keys and serialization`() {
        assertEquals("RedFavoritesTab", assertJavaSerialization(FavoritesTab).key)
        assertEquals("R_Screen_Root", assertJavaSerialization(R_Screen_Root()).key)
        assertEquals("ScreenRedManageBlock", assertJavaSerialization(ScreenRedManageBlock()).key)
        assertEquals("ScreenRedExplorer", assertJavaSerialization(ScreenRedExplorer()).key)
        assertEquals("R_ScreenSavedTab", assertJavaSerialization(R_ScreenSavedTab).key)
        assertEquals("R_Screen_Saved_LikesTab", assertJavaSerialization(R_Screen_Saved_LikesTab).key)
        assertEquals("R_Screen_CreatorsTab", assertJavaSerialization(R_Screen_CreatorsTab).key)
        assertEquals("R_Screen_Saved_DownloadTab", assertJavaSerialization(R_Screen_Saved_DownloadTab).key)
        assertEquals("SavedNichesTab", assertJavaSerialization(SavedNichesTab).key)
        assertEquals("R_Screen_CollectionTab", assertJavaSerialization(R_Screen_CollectionTab).key)
        assertEquals("R_Screen_Saved_SubscriptionsTab", assertJavaSerialization(R_Screen_Saved_SubscriptionsTab).key)
        assertEquals("R_ScreenGifsTab", assertJavaSerialization(R_ScreenGifsTab).key)
        assertEquals("R_ScreenNichesTab", assertJavaSerialization(R_ScreenNichesTab).key)
    }
}
