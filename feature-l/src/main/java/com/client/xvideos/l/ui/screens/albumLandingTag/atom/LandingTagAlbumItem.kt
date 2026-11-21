package com.client.xvideos.l.ui.screens.albumLandingTag.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.model.Album
import com.client.xvideos.l.ui.element.AlbumListItem

@Composable
fun LandingTagAlbumItem(
    album: Album,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val albumId = remember(album.id) { album.id.toLongOrNull() }
    val onCardClick = remember(albumId, onAlbumClick) {
        { if (albumId != null) onAlbumClick(albumId) }
    }
    Box(modifier = modifier) {
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
        onAlbumClick = {}
    )
}
