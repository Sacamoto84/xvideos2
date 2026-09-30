package com.client.xvideos.l.ui.screens.screenAlbumList.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.Album
import com.client.xvideos.l.ui.screens.screenAlbumList.atom.AlbumGridItem
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings

@Composable
fun AlbumListPageGrid(
    stateGrid: LazyGridState,
    pageItems: List<Album>,
    title: String,
    topInset: Dp,
    haptic: HapticFeedback,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGridScrollbar(
        state = stateGrid,
        modifier = modifier,
        settings = ScrollbarSettings.Default.copy(
            thumbUnselectedColor = Color(0xFFA3A3A3),
            thumbSelectedColor = Color(0xFFB3B3B3),
            thumbThickness = 3.dp,
            scrollbarPadding = 0.dp,
            alwaysShowScrollbar = false
        )
    ) {
        LazyVerticalGrid(state = stateGrid, modifier = Modifier.fillMaxSize(), columns = GridCells.Fixed(2)) {
            item(key = "dummy", span = { GridItemSpan(maxLineSpan) }) {
                if (title.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Theme.L.red)
                            .padding(top = topInset)
                            .height(44.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontFamily = Theme.L.fontFamilyKarla,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (topInset > 0.dp) {
                    Spacer(Modifier.height(topInset))
                }
            }

            items(
                items = pageItems,
                key = { it.id },
                contentType = { "album_item" }
            ) { item ->
                AlbumGridItem(
                    item = item,
                    haptic = haptic,
                    onAlbumClick = onAlbumClick
                )
            }
        }
    }
}

@Preview
@Composable
private fun AlbumListPageGridPreview() {
    AlbumListPageGrid(
        stateGrid = rememberLazyGridState(),
        pageItems = listOf(
            Album(id = "1", title = "Album 1", numberOfPictures = 10, numberOfAnimatedPictures = 2),
            Album(id = "2", title = "Album 2", numberOfPictures = 15, numberOfAnimatedPictures = 0)
        ),
        title = "Top Albums",
        topInset = 24.dp,
        haptic = LocalHapticFeedback.current,
        onAlbumClick = {}
    )
}
