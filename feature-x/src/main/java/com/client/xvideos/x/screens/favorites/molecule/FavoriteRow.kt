package com.client.xvideos.x.screens.favorites.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.favorites.atom.DurationOverlay

@Composable
fun FavoriteRow(
    item: ItemsX,
    localUrl: String?,
    posterUrl: String,
    onDelete: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onPlayLocal: (String, ItemsX) -> Unit,
    onOpenVideo: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
    onSaveToGallery: (ItemsX) -> Unit = {},
) {
    val onOpenThisVideo = remember(item, onOpenVideo) { { onOpenVideo(item) } }
    val onDeleteThis = remember(item, onDelete) { { onDelete(item) } }
    val onDownloadThis = remember(item, onDownload) { { onDownload(item) } }
    val onSaveToGalleryThis = remember(item, onSaveToGallery) { { onSaveToGallery(item) } }
    val onPlayLocalThis = remember(localUrl, item, onPlayLocal) {
        localUrl?.let { url -> { onPlayLocal(url, item) } }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp, horizontal = 1.dp)
            .aspectRatio(352f / 198f)
            .background(Color.DarkGray)
    ) {
        when {
            // Скачано: показываем постер, по тапу — локальное воспроизведение полного файла.
            localUrl != null && onPlayLocalThis != null -> {
                UrlImage(
                    posterUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onPlayLocalThis)
                )
                // Значок «скачано» (как в R — IconSave18).
                Row(
                    modifier = Modifier.padding(4.dp)
                ) {
                    IconSave18()
                }
            }

            else -> UrlVideoImageAndLongClickX(
                item,
                onLongClick = onOpenThisVideo,
                onDoubleClick = onOpenThisVideo,
            )
        }

        Row(Modifier.align(Alignment.TopEnd)) {
            FavoriteActionsExpandMenu(
                onDelete = onDeleteThis,
                onDownload = onDownloadThis,
                onSaveToGallery = onSaveToGalleryThis,
            )
        }

        Row(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp)) { DurationOverlay(item.duration) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun FavoriteRowPreview() {
    FavoriteRow(
        item = ItemsX(
            id = 1L,
            title = "Preview Favorite",
            duration = "12:34",
            previewImage = "",
        ),
        localUrl = null,
        posterUrl = "",
        onDelete = {},
        onDownload = {},
        onPlayLocal = { _, _ -> },
        onOpenVideo = {}
    )
}
