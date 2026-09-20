package com.client.xvideos.x.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.vibrate.vibrateWithPatternAndAmplitude
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.XHistoryItem
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import com.composables.core.HorizontalSeparator

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

    HistoryContent(
        history = saved.history.list,
        isFavorite = { saved.favorites.contains(it.id) },
        onToggleFavorite = { item ->
            if (saved.favorites.contains(item.id)) {
                saved.favorites.remove(item)
            } else {
                saved.favorites.add(item)
            }
        },
        localUrlOf = { item ->
            if (item.id in downloadedIds) saved.downloads.localUrl(item.id) else null
        },
        posterUrlOf = { item ->
            if (item.id in downloadedIds) {
                saved.downloads.localPosterPath(item.id) ?: item.previewImage
            } else {
                item.previewImage
            }
        },
        onDelete = { saved.history.delete(it) },
        onClearAll = { saved.history.clearAll() },
        onDownload = { saved.downloads.download(it) },
        onSaveToGallery = { saved.downloads.saveToGallery(it) },
        onPlayLocal = { url, item -> navigator.push(ScreenX_LocalVideoPlayer(url, item)) },
        onOpenVideo = { item -> navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item)) },
        modifier = modifier,
        onDeleteBatch = { items -> saved.history.deleteBatch(items) },
    )
}

@Composable
fun HistoryContent(
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
    val scope = rememberCoroutineScope()

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

    BackHandler(enabled = !isAnyDialogOpen && !isSelectionMode && gridState.firstVisibleItemIndex > 0) {
        scope.launch { gridState.animateScrollToItem(0) }
    }

    LaunchedEffect(history.isEmpty()) {
        if (history.isEmpty() && isSelectionMode) {
            isSelectionMode = false
            selectedIds.clear()
        }
    }

    HistoryDialogs(
        pendingDelete = pendingDelete,
        posterUrlOf = posterUrlOf,
        onConfirmDelete = { onDelete(it); pendingDelete = null },
        onDismissDelete = { pendingDelete = null },
        showClearAllConfirm = showClearAllConfirm,
        onConfirmClearAll = { onClearAll(); showClearAllConfirm = false },
        onDismissClearAll = { showClearAllConfirm = false },
        showBatchDeleteConfirm = showBatchDeleteConfirm,
        batchDeleteCount = selectedIds.size,
        onConfirmBatchDelete = {
            val items = history.map { it.item }.filter { it.id in selectedIds }
            onDeleteBatch(items)
            isSelectionMode = false
            selectedIds.clear()
            showBatchDeleteConfirm = false
        },
        onDismissBatchDelete = { showBatchDeleteConfirm = false },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Theme.L.grey6,
        topBar = {
            HistoryTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedIds.size,
                allSelected = history.isNotEmpty() && selectedIds.size == history.size,
                hasItems = history.isNotEmpty(),
                onEnterSelectionMode = { isSelectionMode = true },
                onExitSelectionMode = {
                    isSelectionMode = false
                    selectedIds.clear()
                },
                onToggleSelectAll = {
                    if (selectedIds.size == history.size) {
                        selectedIds.clear()
                    } else {
                        selectedIds.clear()
                        selectedIds.addAll(history.map { it.item.id })
                    }
                },
                onDeleteBatch = { showBatchDeleteConfirm = true },
                onClearAll = { showClearAllConfirm = true },
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            HistoryEmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.padding(padding),
            ) {
                itemsIndexed(history, key = { index, historyItem -> "${historyItem.item.id}#$index" }) { _, historyItem ->
                    val isSelected = historyItem.item.id in selectedIds
                    val selectionState = HistorySelectionState(
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onToggleSelect = {
                            if (isSelected) {
                                selectedIds.remove(historyItem.item.id)
                            } else {
                                selectedIds.add(historyItem.item.id)
                            }
                        },
                        onStartSelection = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIds.add(historyItem.item.id)
                            }
                        },
                    )

                    HistoryRow(
                        historyItem = historyItem,
                        isFavorite = isFavorite(historyItem.item),
                        onToggleFavorite = { onToggleFavorite(historyItem.item) },
                        localUrl = localUrlOf(historyItem.item),
                        posterUrl = posterUrlOf(historyItem.item),
                        selectionState = selectionState,
                        onDelete = { pendingDelete = historyItem.item },
                        onDownload = { onDownload(historyItem.item) },
                        onSaveToGallery = { onSaveToGallery(historyItem.item) },
                        onPlayLocal = { url -> onPlayLocal(url, historyItem.item) },
                        onOpenVideo = { onOpenVideo(historyItem.item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryDialogs(
    pendingDelete: ItemsX?,
    posterUrlOf: (ItemsX) -> String,
    onConfirmDelete: (ItemsX) -> Unit,
    onDismissDelete: () -> Unit,
    showClearAllConfirm: Boolean,
    onConfirmClearAll: () -> Unit,
    onDismissClearAll: () -> Unit,
    showBatchDeleteConfirm: Boolean,
    batchDeleteCount: Int,
    onConfirmBatchDelete: () -> Unit,
    onDismissBatchDelete: () -> Unit,
) {
    pendingDelete?.let { item ->
        ConfirmDeleteHistoryDialog(
            item = item,
            posterUrl = posterUrlOf(item),
            onConfirm = { onConfirmDelete(item) },
            onDismiss = onDismissDelete,
        )
    }

    if (showClearAllConfirm) {
        ConfirmClearAllHistoryDialog(
            onConfirm = onConfirmClearAll,
            onDismiss = onDismissClearAll,
        )
    }

    if (showBatchDeleteConfirm) {
        ConfirmDeleteBatchHistoryDialog(
            count = batchDeleteCount,
            onConfirm = onConfirmBatchDelete,
            onDismiss = onDismissBatchDelete,
        )
    }
}

@Composable
private fun HistoryTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    hasItems: Boolean,
    onEnterSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onDeleteBatch: () -> Unit,
    onClearAll: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.L.grey6),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Отмена",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = "Выбрано: $selectedCount",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleSelectAll) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                            contentDescription = if (allSelected) "Снять выбор" else "Выбрать все",
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = onDeleteBatch,
                        enabled = selectedCount > 0,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить выбранные",
                            tint = if (selectedCount > 0) Color(0xFFFF5252) else Color.DarkGray,
                        )
                    }
                }
            } else {
                Text(
                    text = "История",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                )
                if (hasItems) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEnterSelectionMode) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "Выбрать",
                                tint = Color.LightGray,
                            )
                        }
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Очистить всё",
                                tint = Color.LightGray,
                            )
                        }
                    }
                }
            }
        }
        HorizontalSeparator(color = Color(0xFF9E9E9E))
    }
}

@Immutable
private data class HistorySelectionState(
    val isSelectionMode: Boolean = false,
    val isSelected: Boolean = false,
    val onToggleSelect: () -> Unit = {},
    val onStartSelection: () -> Unit = {},
)

@Composable
private fun SelectionCheckBadge(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFFE91E63) else Color(0x99000000))
            .border(
                width = 1.5.dp,
                color = if (isSelected) Color.White else Color.LightGray,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Выбрано",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = "История просмотров пуста",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Здесь появятся ролики длительностью от 2 минут",
                color = Color.DarkGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCardMedia(
    item: ItemsX,
    localUrl: String?,
    posterUrl: String,
    selectionState: HistorySelectionState,
    onPlayLocal: (String) -> Unit,
    onOpenVideo: () -> Unit,
) {
    val context = LocalContext.current
    when {
        localUrl != null -> {
            UrlImage(
                posterUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = {
                            if (selectionState.isSelectionMode) {
                                selectionState.onToggleSelect()
                            } else {
                                onPlayLocal(localUrl)
                            }
                        },
                        onLongClick = {
                            vibrateWithPatternAndAmplitude(context = context)
                            selectionState.onStartSelection()
                        }
                    )
            )
            Row(modifier = Modifier.padding(4.dp)) {
                IconSave18()
            }
        }
        else -> UrlVideoImageAndLongClickX(
            item,
            onLongClick = {
                vibrateWithPatternAndAmplitude(context = context)
                if (selectionState.isSelectionMode) {
                    selectionState.onToggleSelect()
                } else {
                    selectionState.onStartSelection()
                }
            },
            onDoubleClick = {
                if (selectionState.isSelectionMode) {
                    selectionState.onToggleSelect()
                } else {
                    onOpenVideo()
                }
            },
        )
    }
}

@Composable
private fun HistoryWatchedBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xCC1B5E20))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "Просмотрено",
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun HistoryRow(
    historyItem: XHistoryItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    localUrl: String?,
    posterUrl: String,
    selectionState: HistorySelectionState,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onPlayLocal: (String) -> Unit,
    onOpenVideo: () -> Unit,
    onSaveToGallery: () -> Unit = {},
) {
    val item = historyItem.item

    val cardBorderModifier = if (selectionState.isSelected) {
        Modifier.border(2.dp, Color(0xFFE91E63))
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp)
            .aspectRatio(352f / 198f)
            .background(Color.DarkGray)
            .then(cardBorderModifier)
    ) {
        HistoryCardMedia(
            item = item,
            localUrl = localUrl,
            posterUrl = posterUrl,
            selectionState = selectionState,
            onPlayLocal = onPlayLocal,
            onOpenVideo = onOpenVideo,
        )

        if (selectionState.isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (selectionState.isSelected) Color(0x55E91E63) else Color.Transparent)
                    .clickable { selectionState.onToggleSelect() }
            )
            SelectionCheckBadge(
                isSelected = selectionState.isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        } else {
            Row(Modifier.align(Alignment.TopEnd)) {
                HistoryActionsMenu(
                    isFavorite = isFavorite,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete,
                    onDownload = onDownload,
                    onSaveToGallery = onSaveToGallery,
                )
            }
        }

        if (historyItem.isCompleted) {
            HistoryWatchedBadge(Modifier.align(Alignment.BottomStart))
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 4.dp)
        ) {
            DurationOverlay(item.duration)
        }

        if (historyItem.lastPositionMs > 0L) {
            LinearProgressIndicator(
                progress = { historyItem.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = Color(0xFFFF3333),
                trackColor = Color(0x66000000),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryActionsMenu(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onSaveToGallery: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
            onClick = {}
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .offset(0.5.dp, 0.5.dp)
            )
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Действия",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Min),
            containerColor = Theme.ExpandMenu.backgroundColor,
        ) {
            ExpandMenuActionItem(
                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                if (isFavorite) "Удалить из избранного" else "В избранное"
            ) {
                onToggleFavorite()
                expanded = false
            }

            ExpandMenuActionItem(Icons.Filled.ArrowCircleDown, "Скачать") {
                onDownload()
                expanded = false
            }

            ExpandMenuActionItem(Icons.Filled.SaveAlt, "В галерею") {
                onSaveToGallery()
                expanded = false
            }

            ExpandMenuActionItem(Icons.Filled.Delete, "Удалить из истории") {
                onDelete()
                expanded = false
            }
        }
    }
}

@Composable
private fun ConfirmDeleteHistoryDialog(
    item: ItemsX,
    posterUrl: String = item.previewImage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LavenderDialog(
        title = "Удалить из истории?",
        onDismiss = onDismiss,
        icon = {
            UrlImage(
                url = posterUrl,
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(352f / 198f)
                    .clip(RoundedCornerShape(8.dp))
            )
        },
        confirmText = "Удалить",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Composable
private fun ConfirmClearAllHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LavenderDialog(
        title = "Очистить историю?",
        content = {
            Text(
                text = "Все записи истории просмотров будут безвозвратно удалены.",
                color = Color.White,
                fontSize = 14.sp,
            )
        },
        onDismiss = onDismiss,
        confirmText = "Очистить",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Composable
private fun ConfirmDeleteBatchHistoryDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LavenderDialog(
        title = "Удалить выбранные?",
        content = {
            Text(
                text = "Будет удалено $count видео из истории просмотров.",
                color = Color.White,
                fontSize = 14.sp,
            )
        },
        onDismiss = onDismiss,
        confirmText = "Удалить",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Composable
private fun DurationOverlay(duration: String) {
    val text = duration.trim().removeSuffix(".")
    if (text.isEmpty()) return
    val offsetY = (-3).dp
    Box(modifier = Modifier) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .offset(1.dp, offsetY + 1.dp),
            textAlign = TextAlign.Right,
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .offset(0.dp, offsetY),
            textAlign = TextAlign.Right,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}
