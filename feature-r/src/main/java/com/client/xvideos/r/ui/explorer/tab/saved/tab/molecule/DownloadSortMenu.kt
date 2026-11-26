package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.common.downloader.DownloadSort
import com.client.xvideos.r.ui.explorer.tab.saved.tab.model.DownloadMenuOption

private val SORT_OPTIONS = listOf(
    DownloadMenuOption(DownloadSort.DATE, "По дате (сначала новые)", Icons.Outlined.CalendarToday),
    DownloadMenuOption(DownloadSort.SIZE_ASCENDING, "По размеру (сначала меньшие)", Icons.Outlined.ArrowUpward),
    DownloadMenuOption(DownloadSort.SIZE_DESCENDING, "По размеру (сначала большие)", Icons.Outlined.ArrowDownward),
)

@Composable
internal fun DownloadSortMenu(sort: DownloadSort, onSortChange: (DownloadSort) -> Unit) {
    DownloadOptionsMenu(
        title = "Сортировка загрузок",
        icon = Icons.AutoMirrored.Filled.Sort,
        options = SORT_OPTIONS,
        selected = sort,
        onSelect = onSortChange,
    )
}

@Preview
@Composable
private fun DownloadSortMenuPreview() {
    DownloadSortMenu(sort = DownloadSort.DATE, onSortChange = {})
}
