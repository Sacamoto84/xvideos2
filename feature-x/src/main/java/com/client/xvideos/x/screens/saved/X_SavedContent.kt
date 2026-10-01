package com.client.xvideos.x.screens.saved

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.export.XExporter
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.saved.atom.SavedHeader
import com.client.xvideos.x.screens.saved.molecule.ConfirmDeleteVideoDialog
import com.client.xvideos.x.screens.saved.molecule.SavedRow
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Контент экрана «Сохранённое» (загруженные превью-mp4).
 *
 * Рендерится инлайн как под-вкладка раздела Savable (рядом с «Избранным»).
 * Список наблюдается из [SavedX] (`downloads.list`), удаление — с подтверждением.
 */
@Composable
fun X_SavedContent(saved: SavedX, modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    val coroutineScope = rememberCoroutineScope()
    val list by saved.downloads.list.collectAsStateWithLifecycle()

    var pendingDelete by remember { mutableStateOf<ItemsX?>(null) }
    val listState = rememberLazyListState()

    val onPlayItem: (ItemsX) -> Unit = remember(navigator, saved.downloads) {
        { item -> navigator.push(ScreenX_LocalVideoPlayer(saved.downloads.localUrl(item.id), item)) }
    }
    val onDeleteItem: (ItemsX) -> Unit = remember {
        { item -> pendingDelete = item }
    }
    val onShareP2pItem: (ItemsX) -> Unit = remember(navigator, coroutineScope) {
        { item ->
            coroutineScope.launch {
                val bundle = withContext(Dispatchers.IO) {
                    XExporter.export(File(AppPath.x_cache_download), item.id)
                }
                if (bundle == null) {
                    SnackBar.error("Нет скачанного видео для P2P")
                } else {
                    navigator.push(ScreenP2pSend(P2pSendSource.Ready(bundle)))
                }
            }
        }
    }

    val onConfirmDelete = remember(saved.downloads) {
        { item: ItemsX ->
            saved.downloads.delete(item)
            pendingDelete = null
        }
    }
    val onDismissDelete = remember { { pendingDelete = null } }

    // Нажатие «Назад» при открытом диалоге закрывает диалог, не переключая вкладку
    BackHandler(enabled = pendingDelete != null, onBack = onDismissDelete)

    pendingDelete?.let { item ->
        val onConfirmItem = remember(item, onConfirmDelete) {
            { onConfirmDelete(item) }
        }
        val dialogImageUrl = remember(item.id, item.previewImage, saved.downloads) {
            saved.downloads.localPosterPath(item.id) ?: item.previewImage
        }
        ConfirmDeleteVideoDialog(
            title = "Удалить из сохранённого?",
            imageUrl = dialogImageUrl,
            onConfirm = onConfirmItem,
            onDismiss = onDismissDelete,
        )
    }

    val topCutout = getTopInsetDp()

    X_SavedList(
        list = list,
        topCutout = topCutout,
        listState = listState,
        onPlayItem = onPlayItem,
        onDeleteItem = onDeleteItem,
        onShareP2pItem = onShareP2pItem,
        posterUrlProvider = { item ->
            saved.downloads.localPosterPath(item.id) ?: item.previewImage
        },
        modifier = modifier
    )
}

@Composable
fun X_SavedList(
    list: List<ItemsX>,
    topCutout: androidx.compose.ui.unit.Dp,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onPlayItem: (ItemsX) -> Unit = {},
    onDeleteItem: (ItemsX) -> Unit = {},
    onShareP2pItem: (ItemsX) -> Unit = {},
    posterUrlProvider: (ItemsX) -> String = { it.previewImage },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.L.grey6)
    ) {
        if (list.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SavedHeader(topCutout = topCutout)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Пусто", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "header", contentType = "header") {
                    SavedHeader(topCutout = topCutout)
                }
                items(
                    items = list,
                    key = { it.id },
                    contentType = { "saved_row" }
                ) { item ->
                    SavedRow(
                        item = item,
                        posterUrl = posterUrlProvider(item),
                        onPlay = onPlayItem,
                        onDelete = onDeleteItem,
                        onShareP2p = onShareP2pItem,
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun X_SavedListPreview() {
    X_SavedList(
        list = emptyList(),
        topCutout = 0.dp
    )
}
