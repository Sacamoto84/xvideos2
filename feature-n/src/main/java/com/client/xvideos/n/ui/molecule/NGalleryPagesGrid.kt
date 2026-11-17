package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveListPadding
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.n.model.NPage

/**
 * Сетка миниатюр всех страниц манги с поддержкой заголовка (header).
 */
@Composable
fun NGalleryPagesGrid(
    pages: List<NPage>,
    mediaId: Long,
    columns: Int,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    menuContent: (@Composable (page: NPage, index: Int) -> Unit)? = null
) {
    val pageShape = RoundedCornerShape(6.dp)

    LazyVerticalGrid(
        columns = GridCells.Fixed(adaptiveUserGridColumns(columns)),
        modifier = modifier,
        contentPadding = adaptiveListPadding(PaddingValues(8.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (header != null) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                header()
            }
        }

        itemsIndexed(pages, key = { _, page -> page.pageNumber }) { index, page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(137f / 200f)
                    .clip(pageShape)
                    .border(1.dp, Theme.tabLevel3, pageShape)
                    .background(Theme.tabLevel1)
                    .clickable { onPageClick(index) }
            ) {
                UrlImage(
                    url = page.thumbnailUrl(mediaId),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )

                // Номер страницы в углу
                Text(
                    text = "${page.pageNumber}",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color(0x99000000), RoundedCornerShape(topStart = 4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                // Меню действий (кнопка «три точки») в верхнем правом углу
                if (menuContent != null) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        menuContent(page, index)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NGalleryPagesGridPreview() {
    NGalleryPagesGrid(
        pages = listOf(
            NPage(pageNumber = 1, ext = "jpg"),
            NPage(pageNumber = 2, ext = "jpg"),
            NPage(pageNumber = 3, ext = "jpg")
        ),
        mediaId = 12345L,
        columns = 3,
        onPageClick = {}
    )
}
