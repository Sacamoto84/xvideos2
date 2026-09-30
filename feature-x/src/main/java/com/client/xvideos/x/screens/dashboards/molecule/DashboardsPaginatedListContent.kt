package com.client.xvideos.x.screens.dashboards.molecule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.ui.lazy.viewportFractionCacheWindow
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.model.ItemsX
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalFoundationApi::class)
@Suppress("DEPRECATION")
@Composable
fun DashboardsPaginatedListContent(
    items: ImmutableList<ItemsX>,
    isFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    openVideoPlayer: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit = {},
    gridState: LazyGridState = rememberLazyGridState(cacheWindow = viewportFractionCacheWindow()),
    contentPadding: PaddingValues? = null,
    modifier: Modifier = Modifier,
) {
    val topCutout = getTopInsetDp()
    val actualContentPadding = contentPadding ?: remember(topCutout) { PaddingValues(top = topCutout) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        state = gridState,
        contentPadding = actualContentPadding,
    ) {
        itemsIndexed(
            items = items,
            key = { index, cell -> "${cell.id}#$index" },
            contentType = { _, _ -> "dashboard_cell" }
        ) { _, cell ->
            DashboardGridCell(
                cell = cell,
                isFavorite = isFavorite(cell.id),
                openVideoPlayer = openVideoPlayer,
                onFavoriteAdd = onFavoriteAdd,
                onFavoriteRemove = onFavoriteRemove,
                onDownload = onDownload,
                onSaveToGallery = onSaveToGallery,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DashboardsPaginatedListContentPreview() {
    DashboardsPaginatedListContent(
        items = persistentListOf(
            ItemsX(
                id = 1L,
                title = "Sample Video 1",
                duration = "10:00",
                channel = "TopChannel"
            ),
            ItemsX(
                id = 2L,
                title = "Sample Video 2",
                duration = "05:30",
                channel = "Channel 2"
            )
        ),
        isFavorite = { false },
        onFavoriteAdd = {},
        onFavoriteRemove = {},
        onDownload = {},
        openVideoPlayer = {}
    )
}
