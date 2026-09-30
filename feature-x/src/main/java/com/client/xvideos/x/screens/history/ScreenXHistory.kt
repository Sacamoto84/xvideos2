package com.client.xvideos.x.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.XHistoryItem
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.history.atom.HistoryEmptyState
import com.client.xvideos.x.screens.history.model.HistoryDialogData
import com.client.xvideos.x.screens.history.molecule.HistoryDialogHost
import com.client.xvideos.x.screens.history.molecule.HistoryGrid
import com.client.xvideos.x.screens.history.model.HistoryRowActions
import com.client.xvideos.x.screens.history.molecule.HistoryTopBarHost
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer

/**
 * Контент экрана «История просмотров» раздела X.
 *
 * Отображается как 3-я подвкладка в панели Savable (`ScreenXDashBoards`).
 */
@Composable
fun ScreenXHistory(
    saved: SavedX,
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
        localUrlOf = remember(downloadedIds, saved.downloads) {
            { item ->
                if (item.id in downloadedIds) saved.downloads.localUrl(item.id) else null
            }
        },
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
        onPlayLocal = remember(navigator) { { url, item -> navigator.push(ScreenX_LocalVideoPlayer(url, item)) } },
        onOpenVideo = remember(navigator) { { item -> navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item)) } },
        modifier = modifier,
        onDeleteBatch = remember(saved.history) { { items -> saved.history.deleteBatch(items) } },
    )
}

/**
 * Корневая компоновка экрана истории просмотров.
 */
@Composable
fun ScreenXHistoryContent(
    history: List<XHistoryItem>,
    isFavorite: (ItemsX) -> Boolean,
    onToggleFavorite: (ItemsX) -> Unit,
    localUrlOf: (ItemsX) -> String?,
    posterUrlOf: (ItemsX) -> String,
    onDelete: (ItemsX) -> Unit,
    onClearAll: () -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    onPlayLocal: (String, ItemsX) -> Unit,
    onOpenVideo: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteBatch: (Collection<ItemsX>) -> Unit = {},
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var pendingDelete by remember { mutableStateOf<ItemsX?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    val isAnyDialogOpen = pendingDelete != null || showClearAllConfirm || showBatchDeleteConfirm
    BackHandler(enabled = isAnyDialogOpen) {
        pendingDelete = null
        showClearAllConfirm = false
        showBatchDeleteConfirm = false
    }

    BackHandler(enabled = !isAnyDialogOpen && isSelectionMode) {
        isSelectionMode = false
        selectedIds.clear()
    }

    LaunchedEffect(history.isEmpty()) {
        if (history.isEmpty() && isSelectionMode) {
            isSelectionMode = false
            selectedIds.clear()
        }
    }

    val dialogData = HistoryDialogData(
        pendingDelete = pendingDelete,
        showClearAllConfirm = showClearAllConfirm,
        showBatchDeleteConfirm = showBatchDeleteConfirm,
        selectedIds = selectedIds,
    )
    val onDismissDelete = remember { { pendingDelete = null } }
    val onDismissClearAll = remember { { showClearAllConfirm = false } }
    val onDismissBatchDelete = remember { { showBatchDeleteConfirm = false } }
    val onBatchDeleteExecuted = remember(selectedIds, onDeleteBatch) {
        { items: Collection<ItemsX> ->
            onDeleteBatch(items)
            isSelectionMode = false
            selectedIds.clear()
            showBatchDeleteConfirm = false
        }
    }

    HistoryDialogHost(
        dialogData = dialogData,
        posterUrlOf = posterUrlOf,
        onDelete = onDelete,
        onClearAll = onClearAll,
        onDeleteBatch = onBatchDeleteExecuted,
        history = history,
        onDismissDelete = onDismissDelete,
        onDismissClearAll = onDismissClearAll,
        onDismissBatchDelete = onDismissBatchDelete,
    )

    val onToggleSelect: (Long) -> Unit = remember(selectedIds) {
        { id -> if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id) }
    }
    val onStartSelection: (Long) -> Unit = remember(selectedIds) {
        { id -> isSelectionMode = true; if (id !in selectedIds) selectedIds.add(id) }
    }
    val onDeleteRequest: (ItemsX) -> Unit = remember { { item -> pendingDelete = item } }

    val actions = remember(onToggleFavorite, onDeleteRequest, onDownload, onPlayLocal, onOpenVideo, onSaveToGallery) {
        HistoryRowActions(
            onToggleFavorite = onToggleFavorite,
            onDelete = onDeleteRequest,
            onDownload = onDownload,
            onPlayLocal = onPlayLocal,
            onOpenVideo = onOpenVideo,
            onSaveToGallery = onSaveToGallery,
        )
    }

    val onDeleteBatchRequest = remember { { showBatchDeleteConfirm = true } }
    val onClearAllRequest = remember { { showClearAllConfirm = true } }
    val onSetSelectionMode = remember { { mode: Boolean -> isSelectionMode = mode } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Theme.L.grey6,
        topBar = {
            HistoryTopBarHost(
                isSelectionMode = isSelectionMode,
                selectedIds = selectedIds,
                history = history,
                onSetSelectionMode = onSetSelectionMode,
                onDeleteBatch = onDeleteBatchRequest,
                onClearAll = onClearAllRequest,
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            HistoryEmptyState(modifier = Modifier.padding(padding))
        } else {
            HistoryGrid(
                history = history,
                gridState = gridState,
                selectedIds = selectedIds,
                isSelectionMode = isSelectionMode,
                isFavorite = isFavorite,
                localUrlOf = localUrlOf,
                posterUrlOf = posterUrlOf,
                actions = actions,
                onToggleSelect = onToggleSelect,
                onStartSelection = onStartSelection,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
