package com.client.xvideos.screenSettings.components

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** «Хранилище» в настройках считает раздел XLive отдельно — с записями эфиров. */
class StorageStatisticsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun File.writeFile(relative: String, bytes: Int) {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(bytes))
    }

    @Test
    fun `XLive — своя строка с записями, X его не считает`() {
        val root = tmp.newFolder("store")
        root.writeFile("X/Favorites/a.ItemsX", 3)
        root.writeFile("XLive/Records/1/video.mp4", 5)
        root.writeFile("XLive/Saved/a.room", 2)

        val stats = loadStorageStats(root).associateBy { it.key }

        assertEquals(listOf("X", "L", "R", "XLive"), loadStorageStats(root).map { it.key })
        assertEquals(7L, stats.getValue("XLive").sizeBytes)
        assertEquals(2, stats.getValue("XLive").fileCount)
        assertEquals(3L, stats.getValue("X").sizeBytes)
    }

    @Test
    fun `папки раздела — первый уровень, большие первыми, файлы в корне последними`() {
        val root = tmp.newFolder("store")
        root.writeFile("R/Likes/a.json", 2)
        root.writeFile("R/Download/creator/1.mp4", 10)
        root.writeFile("R/Download/creator/1.info", 1)
        root.writeFile("R/top.json", 20)
        File(root, "R/Empty").mkdirs()

        val r = loadStorageStats(root).single { it.key == "R" }

        assertEquals(
            listOf(
                StorageFolderStat("Download", 11L, 2),
                StorageFolderStat("Likes", 2L, 1),
                StorageFolderStat("Empty", 0L, 0),
                StorageFolderStat(null, 20L, 1)
            ),
            r.folders
        )
        assertEquals(33L, r.sizeBytes)
        assertEquals(4, r.fileCount)
    }

    @Test
    fun `нет папки раздела — нет и списка папок`() {
        val root = tmp.newFolder("store")

        val l = loadStorageStats(root).single { it.key == "L" }

        assertEquals(emptyList<StorageFolderStat>(), l.folders)
        assertEquals(0L, l.sizeBytes)
    }
}
