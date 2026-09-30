package com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.atom.SubscribedAlbumGridItem

@Composable
fun SubscribedAlbumsGrid(
    state: LazyGridState,
    albums: List<AlbumDetails>,
    topInset: Dp,
    isLoading: Boolean,
    onAlbumClick: (Long?) -> Unit,
    onAlbumLongClick: (AlbumDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        item(key = "top_inset", contentType = "top_inset", span = { GridItemSpan(maxLineSpan) }) {
            Box(modifier = Modifier.height(topInset))
        }

        items(albums, key = { it.id }, contentType = { "album_item" }) { item ->
            SubscribedAlbumGridItem(
                item = item,
                onAlbumClick = onAlbumClick,
                onAlbumLongClick = onAlbumLongClick,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            )
        }

        if (isLoading && albums.isNotEmpty()) {
            item(key = "loading_indicator", contentType = "loading_indicator", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Theme.L.red)
                }
            }
        }
    }
}

@Preview
@Composable
private fun SubscribedAlbumsGridPreview() {
    SubscribedAlbumsGrid(
        state = rememberLazyGridState(),
        albums = listOf(
            AlbumDetails(id = "1", title = "Album 1"),
            AlbumDetails(id = "2", title = "Album 2")
        ),
        topInset = 24.dp,
        isLoading = false,
        onAlbumClick = {},
        onAlbumLongClick = {}
    )
}
