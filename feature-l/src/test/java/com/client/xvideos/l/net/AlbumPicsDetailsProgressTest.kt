package com.client.xvideos.l.net

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AlbumPicsDetailsProgressTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_l").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    @Test
    fun initial_state_has_zero_percent_and_null_snapshot() = runTest {
        val details = AlbumPicsDetails(123, Repository(AppFileDatabase()))
        assertEquals(0f, details.percentLoad)
        assertNull(details.bundleSnapshotOrNull())
    }

    @Test
    fun restoreFromBundleCache_sets_complete_percent_and_valid_snapshot() = runTest {
        val details = AlbumPicsDetails(123, Repository(AppFileDatabase()))
        val items = listOf(
            PicsDetails(
                height = 100,
                width = 100,
                is_animated = false,
                url_to_original = "https://example.com/pic1.jpg"
            )
        )

        details.restoreFromBundleCache(items, cachedTotalPages = 3)

        assertEquals(1f, details.percentLoad)
        assertEquals(3, details.totalPages)
        assertEquals(1, details.pics.size)

        val snapshot = details.bundleSnapshotOrNull()
        assertNotNull(snapshot)
        assertEquals(3, snapshot?.totalPages)
        assertEquals(1, snapshot?.pics?.size)
    }
}
