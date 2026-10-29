package com.client.xvideos.x.screens.saved.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.feature.saved.SavedLocalVideo
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX

/**
 * Карточка скачанного ролика: на превью размер файла и длительность, под ним
 * кнопки. Название ролика не показывается: у скачанных в нём обычно мусор.
 * Размер — на превью, а не рядом с кнопками: в две колонки на телефоне
 * рядом с тремя кнопками остаётся около 35dp, и подпись обрезалась.
 */
@Composable
fun SavedRow(
    item: ItemsX,
    localVideo: SavedLocalVideo?,
    onOpen: (ItemsX) -> Unit,
    onDelete: (ItemsX) -> Unit,
    onShareP2p: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(item, onOpen) { { onOpen(item) } }
    val handleSaveToGallery = remember(item, onSaveToGallery) { { onSaveToGallery(item) } }
    val handleShareP2p = remember(item, onShareP2p) { { onShareP2p(item) } }
    val handleDelete = remember(item, onDelete) { { onDelete(item) } }
    val sizeBytes = localVideo?.sizeBytes ?: 0L
    val sizeLabel = remember(sizeBytes) { if (sizeBytes > 0L) formatBytes(sizeBytes) else "" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(352f / 198f)
                .background(Color.DarkGray)
        ) {
            // Тап — превью из скачанного файла, удержание и двойной тап — плеер.
            UrlVideoImageAndLongClickX(
                item,
                onLongClick = handleOpen,
                onDoubleClick = handleOpen,
                localVideo = localVideo,
            )

            // Размер файла — в левом верхнем углу, напротив длительности.
            Text(
                text = sizeLabel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                fontSize = 14.sp,
                color = Color.White
            )

            // Продолжительность видео в правом верхнем углу.
            Text(
                text = item.duration,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                textAlign = TextAlign.Right,
                fontSize = 14.sp,
                color = Color.White
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.L.grey6),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = handleSaveToGallery) {
                Icon(
                    imageVector = Icons.Filled.SaveAlt,
                    contentDescription = "Сохранить в галерею",
                    tint = Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(onClick = handleShareP2p) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "P2P",
                    tint = Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(onClick = handleDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Удалить",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun SavedRowPreview() {
    SavedRow(
        item = ItemsX(
            id = 1L,
            title = "Preview Title",
            duration = "10:00"
        ),
        localVideo = SavedLocalVideo(fileUrl = "file:///v.mp4", posterPath = null, sizeBytes = 257_000_000L),
        onOpen = {},
        onDelete = {},
        onShareP2p = {},
        onSaveToGallery = {}
    )
}
