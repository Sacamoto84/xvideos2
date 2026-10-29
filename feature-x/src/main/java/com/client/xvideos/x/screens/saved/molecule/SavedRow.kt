package com.client.xvideos.x.screens.saved.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.feature.saved.SavedLocalVideo
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.saved.atom.SavedOverlayText

/**
 * Карточка скачанного ролика — как в «Избранном»: только превью. Справа
 * сверху «⋮» (в галерею, P2P, удалить), снизу слева размер файла, справа
 * длительность — белым с тенью; пока по тапу играет превью, их нет. Название
 * ролика не показывается: у скачанных в нём обычно мусор.
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
    val durationLabel = remember(item.duration) { item.duration.trim().removeSuffix(".") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .aspectRatio(352f / 198f)
            .background(Color.DarkGray)
    ) {
        // Тап — превью из скачанного файла, удержание и двойной тап — плеер.
        // Подписи и «⋮» — в overlay: пока играет превью, их нет, как в ленте.
        UrlVideoImageAndLongClickX(
            item,
            onLongClick = handleOpen,
            onDoubleClick = handleOpen,
            localVideo = localVideo,
        ) {
            SavedOverlayText(
                text = sizeLabel,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 4.dp)
            )

            SavedOverlayText(
                text = durationLabel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 4.dp)
            )

            SavedActionsMenu(
                onSaveToGallery = handleSaveToGallery,
                onShareP2p = handleShareP2p,
                onDelete = handleDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            )
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
