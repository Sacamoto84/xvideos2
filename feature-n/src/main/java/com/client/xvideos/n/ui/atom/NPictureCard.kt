package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.NCardLayout

/**
 * Карточка сохранённой картинки / страницы манги для сеток лайков и коллекций.
 */
@Composable
fun NPictureCard(
    picture: NPicture,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: (@Composable () -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(NCardLayout.aspectRatio)
            .border(1.dp, Theme.tabLevel3, cardShape)
            .clip(cardShape)
            .background(Theme.tabLevel1)
            .clickable(onClick = onClick)
    ) {
        UrlImage(
            picture.thumbnailUrl,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit
        )

        // Нижняя плашка с названием манги и страницей
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            if (picture.galleryTitle.isNotEmpty()) {
                Text(
                    text = picture.galleryTitle,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Стр. ${picture.pageNumber}",
                color = Color(0xFFED2553),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Меню действий (кнопка «три точки») в верхнем правом углу
        if (menuContent != null) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                menuContent()
            }
        }
    }
}

@Preview
@Composable
private fun NPictureCardPreview() {
    NPictureCard(
        picture = NPicture(
            id = "123_4",
            galleryId = 123L,
            mediaId = 456L,
            pageNumber = 4,
            galleryTitle = "Preview Manga Title"
        ),
        onClick = {},
        menuContent = null
    )
}
