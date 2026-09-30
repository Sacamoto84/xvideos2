package com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.atom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.ui.element.AlbumListItem

@Composable
fun SubscribedAlbumGridItem(
    item: AlbumDetails,
    onAlbumClick: (Long?) -> Unit,
    onAlbumLongClick: (AlbumDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
    val albumId = remember(item.id) { item.id.toLongOrNull() }
    val coverUrl = remember(item.cover) { item.cover?.url.orEmpty() }
    val onClick = remember(albumId, onAlbumClick) { { onAlbumClick(albumId) } }
    val onLongClick = remember(item, onAlbumLongClick) { { onAlbumLongClick(item) } }
    AlbumListItem(
        title = item.title,
        coverUrl = coverUrl,
        numberOfAnimatedPictures = item.number_of_animated_pictures,
        numberOfPictures = item.number_of_pictures,
        modifier = modifier,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

@Preview
@Composable
private fun SubscribedAlbumGridItemPreview() {
    SubscribedAlbumGridItem(
        item = AlbumDetails(
            id = "1",
            title = "Sample Subscribed Album",
            number_of_pictures = 12,
            number_of_animated_pictures = 3
        ),
        onAlbumClick = {},
        onAlbumLongClick = {}
    )
}
