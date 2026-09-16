package com.client.xvideos.l.net

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AlbumInfoRefreshTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_album_info").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    @Test
    fun refresh_deletes_cached_bundle_from_database() = runBlocking {
        val fileDb = AppFileDatabase()
        val repository = Repository(fileDb)
        val albumId = 98765

        val initialBundle = LAlbumBundleCache(
            schemaVersion = L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION,
            cachedAtMs = System.currentTimeMillis(),
            album = AlbumDetails(id = albumId.toString(), title = "Cached Album"),
            totalPages = 1,
            pics = listOf(
                PicsDetails(
                    height = 100,
                    width = 100,
                    is_animated = false,
                    url_to_original = "https://example.com/item1.jpg"
                )
            )
        )

        repository.putAlbumBundleCache(albumId, LJson.encodeToString(initialBundle))
        assertNotNull(repository.getAlbumBundleCache(albumId, L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS))

        val albumInfo = AlbumInfo(
            id = albumId,
            download = false,
            repository = repository,
            scope = this
        )

        withTimeout(5000) {
            while (albumInfo.albumInfo.value == null) {
                delay(20)
            }
        }

        assertEquals("Cached Album", albumInfo.albumInfo.value?.title)
        assertEquals(1, albumInfo.albumPicsDetails.pics.size)

        albumInfo.refresh()

        withTimeout(5000) {
            while (repository.getAlbumBundleCache(albumId, L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS) != null) {
                delay(20)
            }
        }

        val cachedAfterRefresh = repository.getAlbumBundleCache(albumId, L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS)
        assertNull("Кэш альбома в БД должен быть удален при refresh()", cachedAfterRefresh)
    }
}
