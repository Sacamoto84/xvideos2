package com.client.xvideos.x.screens.history.molecule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.x.feature.saved.SavedLocalVideo
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX

/**
 * Медиа-контент карточки истории: тап — превью (у скачанного — из файла),
 * удержание и двойной тап — плеер (у скачанного — скачанный файл, решает
 * [onOpenVideo] экрана). В режиме выбора у скачанного — постер, и тап с
 * удержанием отмечают ролик.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryCardMedia(
    item: ItemsX,
    localVideo: SavedLocalVideo?,
    posterUrl: String,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onOpenVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Долгое нажатие открывает ролик, как в остальных сетках X, а в режиме выбора
    // отмечает его. Сам режим включает кнопка «Выбрать» в шапке: раньше его включало
    // долгое нажатие, и открыть ролик на весь экран было нельзя.
    val onOpenOrSelect = remember(isSelectionMode, onToggleSelect, onOpenVideo) {
        {
            if (isSelectionMode) {
                onToggleSelect()
            } else {
                onOpenVideo()
            }
        }
    }

    if (isSelectionMode && localVideo != null) {
        UrlImage(
            posterUrl,
            modifier = modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onToggleSelect,
                    onLongClick = onToggleSelect
                )
        )
    } else {
        UrlVideoImageAndLongClickX(
            item,
            modifier = modifier,
            onLongClick = onOpenOrSelect,
            onDoubleClick = onOpenOrSelect,
            localVideo = localVideo,
        )
    }
    if (localVideo != null) {
        Row(modifier = Modifier.padding(4.dp)) {
            IconSave18()
        }
    }
}

@Preview
@Composable
private fun HistoryCardMediaPreview() {
    HistoryCardMedia(
        item = ItemsX(id = 1L, title = "Test"),
        localVideo = null,
        posterUrl = "",
        isSelectionMode = false,
        onToggleSelect = {},
        onOpenVideo = {}
    )
}
