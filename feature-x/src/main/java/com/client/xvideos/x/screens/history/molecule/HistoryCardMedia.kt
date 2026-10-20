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
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX

/**
 * Медиа-контент карточки истории (превью видео, локальное изображение или сетевое).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryCardMedia(
    item: ItemsX,
    localUrl: String?,
    posterUrl: String,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onPlayLocal: (String) -> Unit,
    onOpenVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Долгое нажатие открывает ролик, как в остальных сетках X, а в режиме выбора
    // отмечает его. Сам режим включает кнопка «Выбрать» в шапке: раньше его включало
    // долгое нажатие, и открыть ролик на весь экран было нельзя.
    val onCombinedClick = remember(isSelectionMode, onToggleSelect, onPlayLocal, localUrl) {
        {
            if (isSelectionMode) {
                onToggleSelect()
            } else if (localUrl != null) {
                onPlayLocal(localUrl)
            }
        }
    }
    val onRemoteOpen = remember(isSelectionMode, onToggleSelect, onOpenVideo) {
        {
            if (isSelectionMode) {
                onToggleSelect()
            } else {
                onOpenVideo()
            }
        }
    }

    when {
        localUrl != null -> {
            UrlImage(
                posterUrl,
                modifier = modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = onCombinedClick,
                        onLongClick = onCombinedClick
                    )
            )
            Row(modifier = Modifier.padding(4.dp)) {
                IconSave18()
            }
        }
        else -> UrlVideoImageAndLongClickX(
            item,
            modifier = modifier,
            onLongClick = onRemoteOpen,
            onDoubleClick = onRemoteOpen,
        )
    }
}

@Preview
@Composable
private fun HistoryCardMediaPreview() {
    HistoryCardMedia(
        item = ItemsX(id = 1L, title = "Test"),
        localUrl = null,
        posterUrl = "",
        isSelectionMode = false,
        onToggleSelect = {},
        onPlayLocal = {},
        onOpenVideo = {}
    )
}
