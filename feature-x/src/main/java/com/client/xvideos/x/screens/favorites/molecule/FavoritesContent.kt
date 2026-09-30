package com.client.xvideos.x.screens.favorites.molecule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.favorites.atom.FavoritesHeader

/**
 * Stateless-тело экрана «Избранное» — пригодно для [Preview] (без Hilt/Navigator).
 *
 * @param localUrlOf для скачанного видео возвращает `file://`-URL локального файла, иначе null.
 * @param onPlayLocal открыть локальное воспроизведение по `file://`-URL.
 * @param onOpenVideo открыть сетевой плеер для нескачанного видео.
 */
@Composable
fun FavoritesContent(
    favorites: List<ItemsX>,
    localUrlOf: (ItemsX) -> String?,
    posterUrlOf: (ItemsX) -> String,
    onDelete: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onPlayLocal: (String, ItemsX) -> Unit,
    onOpenVideo: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
    onSaveToGallery: (ItemsX) -> Unit = {},
) {
    // Подтверждение удаления из избранного (диалог).
    var pendingDelete by remember { mutableStateOf<ItemsX?>(null) }
    val onDeleteItem: (ItemsX) -> Unit = remember { { item -> pendingDelete = item } }
    val onDismissDeleteDialog: () -> Unit = remember { { pendingDelete = null } }
    val onConfirmDeleteDialog: (ItemsX) -> Unit = remember(onDelete) {
        { item ->
            onDelete(item)
            pendingDelete = null
        }
    }
    val gridState = rememberLazyGridState()

    // Нажатие «Назад» при открытом диалоге закрывает диалог, не переключая вкладку
    BackHandler(enabled = pendingDelete != null, onBack = onDismissDeleteDialog)

    pendingDelete?.let { item ->
        val onConfirmThis = remember(item, onConfirmDeleteDialog) {
            { onConfirmDeleteDialog(item) }
        }
        ConfirmDeleteFavoriteDialog(
            item = item,
            posterUrl = posterUrlOf(item),
            onConfirm = onConfirmThis,
            onDismiss = onDismissDeleteDialog,
        )
    }

    val topCutout = getTopInsetDp()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Theme.L.grey6
    ) { padding ->

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FavoritesHeader(topCutout = topCutout)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Пусто", color = Color.Gray, fontSize = 16.sp)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                item(key = "header", contentType = "header", span = { GridItemSpan(maxLineSpan) }) {
                    FavoritesHeader(topCutout = topCutout)
                }

                items(items = favorites, key = { item -> item.id }, contentType = { "favorite_row" }) { item ->
                    FavoriteRow(
                        item = item,
                        localUrl = localUrlOf(item),
                        posterUrl = posterUrlOf(item),
                        onDelete = onDeleteItem,
                        onDownload = onDownload,
                        onSaveToGallery = onSaveToGallery,
                        onPlayLocal = onPlayLocal,
                        onOpenVideo = onOpenVideo,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun FavoritesContentPreview() {
    FavoritesContent(
        favorites = listOf(
            ItemsX(
                id = 1L,
                title = "Sample video with a fairly long title to test wrapping",
                duration = "12:34",
                views = "1.2M",
                channel = "Old4k",
                href = "/video1",
                nameProfile = "Old4k",
                linkProfile = "/old4k",
            )
        ),
        localUrlOf = { null },
        posterUrlOf = { it.previewImage },
        onDelete = {},
        onDownload = {},
        onPlayLocal = { _, _ -> },
        onOpenVideo = {},
    )
}
