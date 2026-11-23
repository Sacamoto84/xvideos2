package com.client.xvideos.r.common.downloader

import com.client.xvideos.r.model.GifsInfo

internal enum class DownloadSort {
    DATE,
    SIZE_ASCENDING,
    SIZE_DESCENDING,
}

internal fun RedDownloadsScan.sortedItems(sort: DownloadSort): List<GifsInfo> {
    val sizeOrder = when (sort) {
        DownloadSort.DATE -> return list
        DownloadSort.SIZE_ASCENDING -> nullsLast<Long>()
        DownloadSort.SIZE_DESCENDING -> nullsLast(reverseOrder<Long>())
    }
    // sortedWith сохраняет порядок по дате для записей с равным размером.
    return list.sortedWith(compareBy(sizeOrder, ::videoSize))
}
