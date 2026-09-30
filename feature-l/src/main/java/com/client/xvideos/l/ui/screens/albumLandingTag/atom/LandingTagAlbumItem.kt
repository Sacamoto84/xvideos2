package com.client.xvideos.l.ui.screens.albumLandingTag.atom

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
fun LandingTagAlbumItem(
    album: Album,
    itemWidth: Dp,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val albumId = remember(album.id) { album.id.toLongOrNull() }
    val onCardClick = remember(albumId, onAlbumClick) {
        { if (albumId != null) onAlbumClick(albumId) }
    }
    Box(
        modifier = modifier
            .width(itemWidth)
            .padding(vertical = 2.dp)
    ) {
        AlbumListItem(
            modifier = Modifier.fillMaxWidth(),
            title = album.title,
            coverUrl = album.cover?.url.orEmpty(),
            numberOfAnimatedPictures = album.numberOfAnimatedPictures,
            numberOfPictures = album.numberOfPictures,
            onClick = onCardClick
        )
    }
}

@Preview
@Composable
private fun LandingTagAlbumItemPreview() {
    LandingTagAlbumItem(
        album = Album(id = "1", title = "Sample Album"),
        itemWidth = 120.dp,
        onAlbumClick = {}
    )
}
