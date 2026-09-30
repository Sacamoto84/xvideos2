package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.icons.IconFavorite18
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.ui.expandMenu.X_DashboardExpandMenu

/**
 * Карточка видеоролика канала в сетке.
 */
@Composable
fun ChannelVideoItem(
    item: ItemsX,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onOpenVideo: (ItemsX) -> Unit,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(item, onOpenVideo) { { onOpenVideo(item) } }
    val handleFavoriteAdd = remember(item, onFavoriteAdd) { { onFavoriteAdd(item) } }
    val handleFavoriteRemove = remember(item, onFavoriteRemove) { { onFavoriteRemove(item) } }
    val handleDownload = remember(item, onDownload) { { onDownload(item) } }
    val handleSaveToGallery = remember(item, onSaveToGallery) { { onSaveToGallery(item) } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(352f / 198f)
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141418))
    ) {
        UrlVideoImageAndLongClickX(
            item = item,
            onLongClick = handleOpen,
            onDoubleClick = handleOpen,
        ) {
            // Длительность, иконка избранного и индикатор скачивания в правом нижнем углу
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isDownloaded) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xB3000000))
                            .padding(2.dp)
                    ) {
                        IconSave18()
                    }
                }
                if (isFavorite) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xB3000000))
                            .padding(2.dp)
                    ) {
                        IconFavorite18()
                    }
                }
                if (item.duration.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xB3000000))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.duration,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            // Количество просмотров в левом нижнем углу
            if (item.views.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xB3000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.views,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDDDDDD)
                    )
                }
            }

            // Меню с тремя точками в правом верхнем углу
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            ) {
                X_DashboardExpandMenu(
                    isFavorite = isFavorite,
                    onFavoriteAdd = handleFavoriteAdd,
                    onFavoriteRemove = handleFavoriteRemove,
                    onDownload = handleDownload,
                    onSaveToGallery = handleSaveToGallery,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF040404)
@Composable
private fun ChannelVideoItemPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        ChannelVideoItem(
            item = ItemsX(
                id = 12345L,
                title = "Sample Video Title",
                duration = "12:34",
                views = "1.2M",
                channel = "Sample Channel",
                href = "/video12345",
                nameProfile = "Sample Channel",
                linkProfile = "/channels/sample",
            ),
            isFavorite = true,
            isDownloaded = true,
            onOpenVideo = {},
            onFavoriteAdd = {},
            onFavoriteRemove = {},
            onDownload = {},
            onSaveToGallery = {},
        )
    }
}
