package com.client.xvideos.r.common.pagin

import android.content.ContextWrapper
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ItemProfilePagingSourceTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("item_profile_test").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    @Test
    fun `load returns empty page immediately for empty or whitespace profile name`() = runTest {
        val block = BlockRed(this)
        val redApi = RedApi(AppFileDatabase())

        val emptySource = ItemProfilePagingSource(
            profileName = "",
            sort = Order.LATEST,
            block = block,
            redApi = redApi
        )
        val whitespaceSource = ItemProfilePagingSource(
            profileName = "   \t\n  ",
            sort = Order.LATEST,
            block = block,
            redApi = redApi
        )

        val params = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 20,
            placeholdersEnabled = false
        )

        val resultEmpty = emptySource.load(params)
        assertTrue(resultEmpty is PagingSource.LoadResult.Page)
        val pageEmpty = resultEmpty as PagingSource.LoadResult.Page
        assertTrue(pageEmpty.data.isEmpty())
        assertNull(pageEmpty.prevKey)
        assertNull(pageEmpty.nextKey)

        val resultWhitespace = whitespaceSource.load(params)
        assertTrue(resultWhitespace is PagingSource.LoadResult.Page)
        val pageWhitespace = resultWhitespace as PagingSource.LoadResult.Page
        assertTrue(pageWhitespace.data.isEmpty())
        assertNull(pageWhitespace.prevKey)
        assertNull(pageWhitespace.nextKey)
    }

    @Test
    fun `getRefreshKey returns null when anchorPosition is null`() {
        val block = BlockRed(CoroutineScope(Dispatchers.Unconfined))
        val redApi = RedApi(AppFileDatabase())
        val source = ItemProfilePagingSource("test_user", Order.LATEST, block, redApi)

        val state = PagingState<Int, GifsInfo>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )

        assertNull(source.getRefreshKey(state))
    }
}
