package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.Album
import com.client.xvideos.l.ui.element.AlbumListItem

@Composable
fun TopHitsAlbumItem(
    album: Album,
    itemWidth: Dp,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = remember(album.id, onAlbumClick) {
        {
            val albumId = album.id.toLongOrNull()
            if (albumId != null) {
                onAlbumClick(albumId)
            }
        }
    }
    Box(
        modifier = modifier.width(itemWidth).padding(vertical = 2.dp)
    ) {
        AlbumListItem(
            modifier = Modifier.fillMaxWidth(),
            title = album.title,
            coverUrl = album.cover?.url.orEmpty(),
            numberOfAnimatedPictures = album.numberOfAnimatedPictures,
            numberOfPictures = album.numberOfPictures,
            onClick = onClick
        )
    }
}

@Preview
@Composable
private fun TopHitsAlbumItemPreview() {
    TopHitsAlbumItem(
        album = Album(id = "1", title = "Sample Album"),
        itemWidth = 120.dp,
        onAlbumClick = {}
    )
}
