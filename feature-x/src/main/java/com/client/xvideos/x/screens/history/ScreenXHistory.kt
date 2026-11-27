package com.client.xvideos.x.screens.history

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.screens.common.playerScreenFor
import com.client.xvideos.x.screens.common.rememberLocalVideoOf
import com.client.xvideos.x.screens.history.molecule.ScreenXHistoryContent

/**
 * Контент экрана «История просмотров» раздела X.
 *
 * Отображается как 3-я подвкладка в панели Savable (`ScreenXDashBoards`).
 *
 * @param gridState Прокрутка сетки из ScreenModel дашбордов.
 */
@Composable
fun ScreenXHistory(
    saved: SavedX,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow
    val downloadedIds by saved.downloads.downloadedVideoIds.collectAsStateWithLifecycle()

    ScreenXHistoryContent(
        history = saved.history.list,
        isFavorite = remember(saved.favorites) { { item -> saved.favorites.contains(item.id) } },
        onToggleFavorite = remember(saved.favorites) {
            { item ->
                if (saved.favorites.contains(item.id)) {
                    saved.favorites.remove(item)
                } else {
                    saved.favorites.add(item)
                }
            }
        },
        // Скачанные: превью и плеер — из файла.
        localVideoOf = rememberLocalVideoOf(saved.downloads),
        posterUrlOf = remember(downloadedIds, saved.downloads) {
            { item ->
                if (item.id in downloadedIds) {
                    saved.downloads.localPosterPath(item.id) ?: item.previewImage
                } else {
                    item.previewImage
                }
            }
        },
        onDelete = remember(saved.history) { { saved.history.delete(it) } },
        onClearAll = remember(saved.history) { { saved.history.clearAll() } },
        onDownload = remember(saved.downloads) { { saved.downloads.download(it) } },
        onSaveToGallery = remember(saved.downloads) { { saved.downloads.saveToGallery(it) } },
        onOpenVideo = remember(navigator, saved.downloads) {
            { item -> navigator.push(playerScreenFor(item, saved.downloads.localVideo(item.id))) }
        },
        gridState = gridState,
        modifier = modifier,
        onDeleteBatch = remember(saved.history) { { items -> saved.history.deleteBatch(items) } },
    )
}
