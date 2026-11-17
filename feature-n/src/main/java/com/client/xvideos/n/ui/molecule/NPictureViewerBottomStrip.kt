package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.n.model.NPicture

/**
 * Нижняя лента миниатюр сохранённых картинок для быстрого перехода.
 */
@Composable
fun NPictureViewerBottomStrip(
    pictures: List<NPicture>,
    currentIndex: Int,
    onPictureSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex in pictures.indices) {
            listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC000000))
            .padding(vertical = 8.dp)
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            itemsIndexed(
                items = pictures,
                key = { _, picture -> picture.id }
            ) { index, picture ->
                val isSelected = index == currentIndex
                val shape = RoundedCornerShape(4.dp)
                val borderColor = if (isSelected) Color(0xFFED2553) else Color.Transparent
                val handleClick = remember(index, onPictureSelected) {
                    { onPictureSelected(index) }
                }

                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .aspectRatio(137f / 200f)
                        .clip(shape)
                        .border(2.dp, borderColor, shape)
                        .clickable(onClick = handleClick)
                ) {
                    UrlImage(
                        url = picture.thumbnailUrl,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NPictureViewerBottomStripPreview() {
    NPictureViewerBottomStrip(
        pictures = listOf(
            NPicture(id = "1_1", galleryId = 1L, mediaId = 1L, pageNumber = 1),
            NPicture(id = "1_2", galleryId = 1L, mediaId = 1L, pageNumber = 2),
            NPicture(id = "1_3", galleryId = 1L, mediaId = 1L, pageNumber = 3)
        ),
        currentIndex = 1,
        onPictureSelected = {}
    )
}
