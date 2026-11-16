package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.ui.atom.NGalleryTagChip

/**
 * Шапка экрана манги: обложка, названия, метаинформация, кнопки действий и теги.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NGalleryHeader(
    gallery: NGallery,
    onReadClick: () -> Unit,
    onTagClick: (NTag) -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    val coverShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Верхний ряд: Обложка слева + Названия и кнопка справа
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .aspectRatio(137f / 200f)
                    .clip(coverShape)
                    .border(1.dp, Theme.tabLevel3, coverShape)
                    .background(Theme.tabLevel1)
            ) {
                UrlImage(
                    url = gallery.coverUrl,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = gallery.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )

                if (gallery.japaneseTitle.isNotEmpty() && gallery.japaneseTitle != gallery.title) {
                    Text(
                        text = gallery.japaneseTitle,
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "${gallery.numPages} страниц • #${gallery.id}",
                    color = Color(0xFFED2553),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onReadClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFED2553)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = "Читать",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFavorite) Color(0xFFED2553).copy(alpha = 0.2f) else Theme.tabLevel2)
                            .border(
                                1.dp,
                                if (isFavorite) Color(0xFFED2553) else Theme.tabLevel3,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = onFavoriteClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isFavorite) "Удалить из закладок" else "В закладки",
                            tint = if (isFavorite) Color(0xFFED2553) else Color.White
                        )
                    }
                }
            }
        }

        // Блок тегов
        if (gallery.tags.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Теги и категории:",
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gallery.tags.forEach { tag ->
                        NGalleryTagChip(tag = tag, onClick = onTagClick)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NGalleryHeaderPreview() {
    NGalleryHeader(
        gallery = NGallery(
            id = 123456L,
            mediaId = 987654L,
            title = "Sample Manga Title for Preview",
            japaneseTitle = "サンプルマンガ",
            numPages = 32,
            numFavorites = 540,
            tags = listOf(
                NTag(id = 1, type = "artist", name = "artist_name", count = 120),
                NTag(id = 2, type = "tag", name = "full color", count = 5300),
                NTag(id = 3, type = "language", name = "english", count = 98000)
            )
        ),
        onReadClick = {},
        onTagClick = {}
    )
}
