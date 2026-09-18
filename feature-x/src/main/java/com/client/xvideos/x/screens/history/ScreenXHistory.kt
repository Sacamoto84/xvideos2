package com.client.xvideos.x.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
) {
    var pendingDelete by remember { mutableStateOf<ItemsX?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    pendingDelete?.let { item ->
        ConfirmDeleteHistoryDialog(
            item = item,
            posterUrl = posterUrlOf(item),
            onConfirm = {
                onDelete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (showClearAllConfirm) {
        ConfirmClearAllHistoryDialog(
            onConfirm = {
                onClearAll()
                showClearAllConfirm = false
            },
            onDismiss = { showClearAllConfirm = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        backgroundColor = Theme.L.grey6,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Theme.L.grey6),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "История",
                        color = Color.White,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    )

                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllConfirm = true },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Очистить всё",
                                tint = Color.LightGray,
                            )
                        }
                    }
                }
                HorizontalSeparator(color = Color(0xFF9E9E9E))
            }
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding),
            ) {
                items(history, key = { it.item.id }) { historyItem ->
                    HistoryRow(
                        historyItem = historyItem,
                        isFavorite = isFavorite(historyItem.item),
                        onToggleFavorite = { onToggleFavorite(historyItem.item) },
                        localUrl = localUrlOf(historyItem.item),
                        posterUrl = posterUrlOf(historyItem.item),
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
private fun HistoryRow(
    historyItem: XHistoryItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    localUrl: String?,
    posterUrl: String,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onPlayLocal: (String) -> Unit,
    onOpenVideo: () -> Unit,
    onSaveToGallery: () -> Unit,
) {
    val item = historyItem.item

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp)
            .aspectRatio(352f / 198f)
            .background(Color.DarkGray)
    ) {
        when {
            localUrl != null -> {
                UrlImage(
                    posterUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPlayLocal(localUrl) }
                )
                Row(modifier = Modifier.padding(4.dp)) {
                    IconSave18()
                }
            }
            else -> UrlVideoImageAndLongClickX(
                item,
                onLongClick = onOpenVideo,
                onDoubleClick = onOpenVideo,
            )
        }

        // Меню действий (3 точки) в правом верхнем углу
        Row(Modifier.align(Alignment.TopEnd)) {
            HistoryActionsMenu(
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onDelete = onDelete,
                onDownload = onDownload,
                onSaveToGallery = onSaveToGallery,
            )
        }

        // Отметка о досмотре до конца (если lastPositionMs == 0L)
        if (historyItem.totalDurationMs > 0L && historyItem.lastPositionMs == 0L) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
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

        // Длительность видео
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 4.dp)
        ) {
            DurationOverlay(item.duration)
        }

        // Полоса прогресса воспроизведения по нижнему краю
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
