package com.client.xvideos.l.ui.screens.screenFullScreen.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.screens.screenFullScreen.atom.FullScreenThumbnailItem

@Composable
fun FullScreenBottomThumbnails(
    visible: Boolean,
    lazyRowState: LazyListState,
    filteredPic: List<PicsDetails>,
    currentIndex: Int,
    albumName: String,
    onThumbnailClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        SwipeableBottomPanel {
            // Ряд на всю высоту панели: щели сверху и снизу одинаковые — рамка
            // выделения превью.
            LazyRow(
                state = lazyRowState,
                modifier = Modifier.fillMaxHeight()
            ) {
                    itemsIndexed(
                        filteredPic,
                        key = { index, item -> "${item.url_to_original}#$index" }
                    ) { index, item ->
                        FullScreenThumbnailItem(
                            item = item,
                            isSelected = index == currentIndex,
                            albumName = albumName,
                            onClick = { onThumbnailClick(index) }
                        )
                    }
                }
        }
    }
}

@Preview
@Composable
private fun FullScreenBottomThumbnailsPreview() {
    FullScreenBottomThumbnails(
        visible = true,
        lazyRowState = rememberLazyListState(),
        filteredPic = listOf(PicsDetails()),
        currentIndex = 0,
        albumName = "test",
        onThumbnailClick = {}
    )
}
