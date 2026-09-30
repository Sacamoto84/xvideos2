package com.client.xvideos.x.screens.history.molecule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.common.vibrate.vibrateWithPatternAndAmplitude
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
    onStartSelection: () -> Unit,
    onPlayLocal: (String) -> Unit,
    onOpenVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val onCombinedClick = remember(isSelectionMode, onToggleSelect, onPlayLocal, localUrl) {
        {
            if (isSelectionMode) {
                onToggleSelect()
            } else if (localUrl != null) {
                onPlayLocal(localUrl)
            }
        }
    }
    val onCombinedLongClick = remember(onStartSelection, context) {
        {
            vibrateWithPatternAndAmplitude(context = context)
            onStartSelection()
        }
    }
    val onRemoteLongClick = remember(isSelectionMode, onToggleSelect, onStartSelection, context) {
        {
            vibrateWithPatternAndAmplitude(context = context)
            if (isSelectionMode) {
                onToggleSelect()
            } else {
                onStartSelection()
            }
        }
    }
    val onRemoteDoubleClick = remember(isSelectionMode, onToggleSelect, onOpenVideo) {
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
                        onLongClick = onCombinedLongClick
                    )
            )
            Row(modifier = Modifier.padding(4.dp)) {
                IconSave18()
            }
        }
        else -> UrlVideoImageAndLongClickX(
            item,
            modifier = modifier,
            onLongClick = onRemoteLongClick,
            onDoubleClick = onRemoteDoubleClick,
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
        onStartSelection = {},
        onPlayLocal = {},
        onOpenVideo = {}
    )
}
