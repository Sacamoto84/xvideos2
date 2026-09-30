package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums.atom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.ui.element.AlbumListItem

@Composable
fun SavedAlbumGridItem(
    item: AlbumDetails,
    onAlbumClick: (AlbumDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = remember(item, onAlbumClick) { { onAlbumClick(item) } }
    AlbumListItem(
        title = item.title,
        coverUrl = item.cover?.url.orEmpty(),
        numberOfAnimatedPictures = item.number_of_animated_pictures,
        numberOfPictures = item.number_of_pictures,
        modifier = modifier,
        onClick = onClick,
    )
}

@Preview
@Composable
private fun SavedAlbumGridItemPreview() {
    SavedAlbumGridItem(
        item = AlbumDetails(
            id = "1",
            title = "Sample Album",
            number_of_pictures = 10,
            number_of_animated_pictures = 2
        ),
        onAlbumClick = {}
    )
}
