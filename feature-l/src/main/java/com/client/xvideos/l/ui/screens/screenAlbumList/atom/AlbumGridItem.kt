package com.client.xvideos.l.ui.screens.screenAlbumList.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.Album
import com.client.xvideos.l.ui.element.AlbumListItem

@Composable
fun AlbumGridItem(
    item: Album,
    haptic: HapticFeedback,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val onClick: () -> Unit = remember(item.id, haptic, onAlbumClick) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            val id = item.id.toLongOrNull()
            if (id != null) {
                onAlbumClick(id)
            }
        }
    }
    Box(
        modifier = modifier.padding(vertical = 2.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        AlbumListItem(
            title = item.title,
            coverUrl = item.cover?.url.orEmpty(),
            numberOfAnimatedPictures = item.numberOfAnimatedPictures,
            numberOfPictures = item.numberOfPictures,
            onClick = onClick
        )
    }
}

@Preview
@Composable
private fun AlbumGridItemPreview() {
    AlbumGridItem(
        item = Album(
            id = "1",
            title = "Preview Album",
            numberOfAnimatedPictures = 5,
            numberOfPictures = 20
        ),
        haptic = LocalHapticFeedback.current,
        onAlbumClick = {}
    )
}
