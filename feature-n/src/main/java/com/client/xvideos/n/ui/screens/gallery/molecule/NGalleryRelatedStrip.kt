package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NGalleryPreview

/**
 * Горизонтальная полоса рекомендаций похожих работ на экране деталей манги.
 */
@Composable
fun NGalleryRelatedStrip(
    relatedGalleries: List<NGalleryPreview>,
    onGalleryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (relatedGalleries.isEmpty()) return

    val cardShape = RoundedCornerShape(6.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Похожие работы",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(relatedGalleries, key = { it.id }) { item ->
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .clip(cardShape)
                        .border(1.dp, Theme.tabLevel3, cardShape)
                        .background(Theme.tabLevel1)
                        .clickable { onGalleryClick(item.id) }
                ) {
                    UrlImage(
                        url = item.thumbnailUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(137f / 200f),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color(0xCC1A1A1A))
                            .padding(4.dp)
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NGalleryRelatedStripPreview() {
    NGalleryRelatedStrip(
        relatedGalleries = listOf(
            NGalleryPreview(id = 1L, title = "Related Manga 1", numPages = 24),
            NGalleryPreview(id = 2L, title = "Related Manga 2", numPages = 30)
        ),
        onGalleryClick = {}
    )
}
