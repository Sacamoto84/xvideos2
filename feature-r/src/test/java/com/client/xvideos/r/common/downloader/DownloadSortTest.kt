package com.client.xvideos.r.common.downloader

import com.client.xvideos.r.model.GifsInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadSortTest {
    @Test
    fun `сортировка по размеру ставит записи без видео в конец и сохраняет порядок равных`() {
        val pending = GifsInfo(id = "pending", userName = "first")
        val medium = GifsInfo(id = "medium", userName = "first")
        val largest = GifsInfo(id = "video", userName = "first")
        val smallest = GifsInfo(id = "video", userName = "second")
        val sameSize = GifsInfo(id = "medium", userName = "second")
        val byDate = listOf(pending, medium, largest, smallest, sameSize)
        val downloads = RedDownloadsScan(
            list = byDate,
            videoSizes = mapOf(
                "first/medium" to 2_000L,
                "first/video" to 4_000_000_000L,
                "second/video" to 900L,
                "second/medium" to 2_000L,
            ),
            totalBytes = 4_000_004_900L,
        )

        assertEquals(byDate, downloads.sortedItems(DownloadSort.DATE))
        assertEquals(
            listOf(smallest, medium, sameSize, largest, pending),
            downloads.sortedItems(DownloadSort.SIZE_ASCENDING),
        )
        assertEquals(
            listOf(largest, medium, sameSize, smallest, pending),
            downloads.sortedItems(DownloadSort.SIZE_DESCENDING),
        )
    }
}
