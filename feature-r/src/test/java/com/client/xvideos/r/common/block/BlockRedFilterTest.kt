package com.client.xvideos.r.common.block

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.r.common.block.useCase.blockItem
import com.client.xvideos.r.common.block.useCase.unblockItem
import com.client.xvideos.r.model.GifsInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

class BlockRedFilterTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    @Test
    fun `refreshListAndBlock filters out blocked items by id and updates blockedIds`() = runTest {
        val blockRed = BlockRed(this)
        blockRed.refresh().join()

        val item1 = GifsInfo(id = "gif_1", userName = "user_a")
        val item2 = GifsInfo(id = "gif_2", userName = "user_b")
        val item3 = GifsInfo(id = "gif_3", userName = "user_c")

        val list = MutableStateFlow(listOf(item1, item2, item3))

        // Initial state: none blocked
        blockRed.refreshListAndBlock(list)
        assertEquals(3, list.value.size)
        assertFalse(blockRed.isBlocked(item2.id))

        // Block item2
        assertTrue(blockItem(item2).isSuccess)
        blockRed.refresh().join()

        assertTrue(blockRed.isBlocked(item2.id))
        assertTrue(blockRed.blockedIds.value.contains(item2.id))
        assertEquals(1, blockRed.blockList.value.size)

        // Filter list: item2 should be removed
        blockRed.refreshListAndBlock(list)
        assertEquals(2, list.value.size)
        assertEquals(listOf(item1, item3), list.value)

        // Unblock item2
        assertTrue(unblockItem(item2).isSuccess)
        blockRed.refresh().join()

        assertFalse(blockRed.isBlocked(item2.id))
        assertFalse(blockRed.blockedIds.value.contains(item2.id))
    }
}
