package com.client.xvideos.r.common.pagin

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class ItemCollectionPagingSourceTest {

    private val testDispatcher = StandardTestDispatcher()

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("item_col_test").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    @Before
    fun setUpDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hasCollection accurately reports non-blank collection name`() = runTest {
        val savedRed = SavedRed(RedApi(AppFileDatabase()), this)

        val withCollection = ItemCollectionPagingSource("Anime", savedRed)
        assertTrue(withCollection.hasCollection)

        val withEmpty = ItemCollectionPagingSource("", savedRed)
        assertFalse(withEmpty.hasCollection)

        val withBlank = ItemCollectionPagingSource("   \t  ", savedRed)
        assertFalse(withBlank.hasCollection)

        val withNull = ItemCollectionPagingSource(null, savedRed)
        assertFalse(withNull.hasCollection)
    }

    @Test
    fun `totalSavedCount calculates aggregate count across saved categories`() = runTest {
        val savedRed = SavedRed(RedApi(AppFileDatabase()), this)
        assertEquals(0, savedRed.totalSavedCount)
    }
}
