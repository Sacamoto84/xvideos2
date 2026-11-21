package com.client.xvideos.n.ui.screens.reader.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.n.model.NPage

/**
 * Нижняя полоса быстрой перемотки страниц манги по миниатюрам.
 */
@Composable
fun NReaderBottomStrip(
    pages: List<NPage>,
    mediaId: Long,
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentPageIndex) {
        if (currentPageIndex in pages.indices) {
            listState.animateScrollToItem(currentPageIndex)
        }
    }

    val thumbShape = RoundedCornerShape(4.dp)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC000000))
    ) {
        itemsIndexed(pages, key = { _, page -> page.pageNumber }) { index, page ->
            val isSelected = index == currentPageIndex
            val borderColor = if (isSelected) Color(0xFFED2553) else Color.Transparent

            HintTooltip(text = "Страница ${page.pageNumber}") {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(60.dp)
                        .clip(thumbShape)
                        .border(2.dp, borderColor, thumbShape)
                        .clickable { onPageSelected(index) }
                ) {
                    UrlImage(
                        url = page.thumbnailUrl(mediaId),
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NReaderBottomStripPreview() {
    NReaderBottomStrip(
        pages = listOf(
            NPage(pageNumber = 1, ext = "jpg"),
            NPage(pageNumber = 2, ext = "jpg"),
            NPage(pageNumber = 3, ext = "jpg")
        ),
        mediaId = 12345L,
        currentPageIndex = 1,
        onPageSelected = {}
    )
}
