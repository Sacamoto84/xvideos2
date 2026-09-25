package com.client.xvideos.x.screens.favorites

import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.client.xvideos.common.util.getTopInsetDp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import com.client.xvideos.x.normalizeXUrl
import com.composables.core.HorizontalSeparator

private const val FAVORITE_CARD_ASPECT_RATIO = 352f / 198f
private const val GRID_COLUMNS = 2
private val GRID_CELLS_FIXED = GridCells.Fixed(GRID_COLUMNS)
private val HEADER_GRID_SPAN: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan = {
    GridItemSpan(maxLineSpan)
}
private const val CONTENT_TYPE_HEADER = "header"
private const val CONTENT_TYPE_FAVORITE_ROW = "favorite_row"
private const val TEXT_EMPTY = "Пусто"
private const val TEXT_FAVORITES_TITLE = "Избранное"
private const val TEXT_DOWNLOAD = "Скачать"
private const val TEXT_IN_GALLERY = "В галерею"
private const val TEXT_DELETE = "Удалить"
private const val TEXT_ACTIONS = "Действия"
private val ZERO_WINDOW_INSETS = WindowInsets(0, 0, 0, 0)
private val ACTION_ICON_BUTTON_SIZE = 48.dp
private val ACTION_ICON_SIZE = 24.dp
private val CARD_PADDING_HORIZONTAL = 1.dp
private val CARD_PADDING_VERTICAL = 1.dp
private val DOWNLOAD_ICON_PADDING = 4.dp
private val HEADER_START_PADDING = 16.dp
private val HEADER_VERTICAL_PADDING = 8.dp
private val DURATION_END_PADDING = 8.dp
private val HEADER_TITLE_SIZE = 24.sp
private val EMPTY_FONT_SIZE = 16.sp
private val DURATION_FONT_SIZE = 14.sp
private val ACTION_ICON_SHADOW_OFFSET = 0.5.dp
private val DURATION_SHADOW_OFFSET = 1.dp
private val SEPARATOR_COLOR = Color(0xFF9E9E9E)
private val durationOffsetY = (-3).dp

private val ICON_MORE_VERT = Icons.Default.MoreVert
private val ICON_DOWNLOAD = Icons.Filled.ArrowCircleDown
private val ICON_SAVE_ALT = Icons.Filled.SaveAlt
private val ICON_DELETE = Icons.Filled.Delete

private val COLOR_BLACK = Color.Black
private val COLOR_WHITE = Color.White
private val COLOR_DARK_GRAY = Color.DarkGray
private val COLOR_GRAY = Color.Gray

private val FONT_WEIGHT_BOLD = FontWeight.Bold

private val ALIGN_TOP_CENTER = Alignment.TopCenter
private val ALIGN_CENTER = Alignment.Center
private val ALIGN_TOP_END = Alignment.TopEnd
private val ALIGN_BOTTOM_END = Alignment.BottomEnd

private val ACTION_BUTTON_SIZE_MODIFIER = Modifier.size(ACTION_ICON_BUTTON_SIZE)
private val ACTION_ICON_SIZE_MODIFIER = Modifier.size(ACTION_ICON_SIZE)
private val ACTION_ICON_SHADOW_MODIFIER = Modifier
    .size(ACTION_ICON_SIZE)
    .offset(ACTION_ICON_SHADOW_OFFSET, ACTION_ICON_SHADOW_OFFSET)
private val MENU_WIDTH_MODIFIER = Modifier.width(IntrinsicSize.Min)
private val DOWNLOAD_ICON_PADDING_MODIFIER = Modifier.padding(DOWNLOAD_ICON_PADDING)
private val DURATION_END_PADDING_MODIFIER = Modifier.padding(end = DURATION_END_PADDING)
private val DURATION_SHADOW_MODIFIER = Modifier
    .fillMaxWidth()
    .offset(DURATION_SHADOW_OFFSET, durationOffsetY + DURATION_SHADOW_OFFSET)
private val DURATION_TEXT_MODIFIER = Modifier
    .fillMaxWidth()
    .offset(0.dp, durationOffsetY)
private val HEADER_TEXT_PADDING = Modifier.padding(start = HEADER_START_PADDING, top = HEADER_VERTICAL_PADDING, bottom = HEADER_VERTICAL_PADDING)
private val TEXT_ALIGN_RIGHT = TextAlign.Right
private val NO_OP_CLICK: () -> Unit = {}

class ScreenFavorites : Screen {

    override val key: ScreenKey = "ScreenFavorites"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenFavoritesSM = getScreenModel()

        // Множество id скачанных видео (реактивно) — для значка и локального воспроизведения.
        val downloadedIds by vm.saved.downloads.downloadedVideoIds.collectAsStateWithLifecycle()

        FavoritesContent(
            favorites = vm.favorites,
            localUrlOf = remember(downloadedIds, vm.saved) {
                { item ->
                    if (item.id in downloadedIds) vm.saved.downloads.localUrl(item.id) else null
                }
            },
            posterUrlOf = remember(downloadedIds, vm.saved) {
                { item ->
                    if (item.id in downloadedIds) (vm.saved.downloads.localPosterPath(item.id) ?: item.previewImage)
                    else item.previewImage
                }
            },
            onDelete = remember(vm) { { item -> vm.removeFavorite(item) } },
            onDownload = remember(vm) { { item -> vm.download(item) } },
            onSaveToGallery = remember(vm) { { item -> vm.saveToGallery(item) } },
            onPlayLocal = remember(navigator) { { url, item -> navigator.push(ScreenX_LocalVideoPlayer(url, item)) } },
            onOpenVideo = remember(navigator) { { item -> navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item)) } },
        )
    }
}

/**
 * Stateless-тело экрана «Избранное» — пригодно для [Preview] (без Hilt/Navigator).
 *
 * @param localUrlOf для скачанного видео возвращает `file://`-URL локального файла, иначе null.
 * @param onPlayLocal открыть локальное воспроизведение по `file://`-URL.
 * @param onOpenVideo открыть сетевой плеер для нескачанного видео.
 */
@Composable
private fun FavoritesContent(
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
        contentWindowInsets = ZERO_WINDOW_INSETS,
        containerColor = Theme.L.grey6
    ) { padding ->

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
                contentAlignment = ALIGN_TOP_CENTER
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FavoritesHeader(topCutout = topCutout)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = ALIGN_CENTER) {
                        Text(TEXT_EMPTY, color = COLOR_GRAY, fontSize = EMPTY_FONT_SIZE)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GRID_CELLS_FIXED,
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                item(key = CONTENT_TYPE_HEADER, contentType = CONTENT_TYPE_HEADER, span = HEADER_GRID_SPAN) {
                    FavoritesHeader(topCutout = topCutout)
                }

                items(items = favorites, key = { item -> item.id }, contentType = { CONTENT_TYPE_FAVORITE_ROW }) { item ->
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

@Composable
private fun FavoritesHeader(
    topCutout: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topCutout)
    ) {
        Text(
            TEXT_FAVORITES_TITLE,
            color = COLOR_WHITE,
            fontSize = HEADER_TITLE_SIZE,
            fontWeight = FONT_WEIGHT_BOLD,
            modifier = HEADER_TEXT_PADDING
        )
        HorizontalSeparator(color = SEPARATOR_COLOR)
    }
}

private val FAVORITE_CARD_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(vertical = CARD_PADDING_VERTICAL, horizontal = CARD_PADDING_HORIZONTAL)
    .aspectRatio(FAVORITE_CARD_ASPECT_RATIO)
    .background(COLOR_DARK_GRAY)

@Composable
private fun FavoriteRow(
    item: ItemsX,
    localUrl: String?,
    posterUrl: String,
    onDelete: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onPlayLocal: (String, ItemsX) -> Unit,
    onOpenVideo: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
    onSaveToGallery: (ItemsX) -> Unit = {},
) {
    val onOpenThisVideo = remember(item, onOpenVideo) { { onOpenVideo(item) } }
    val onDeleteThis = remember(item, onDelete) { { onDelete(item) } }
    val onDownloadThis = remember(item, onDownload) { { onDownload(item) } }
    val onSaveToGalleryThis = remember(item, onSaveToGallery) { { onSaveToGallery(item) } }
    val onPlayLocalThis = remember(localUrl, item, onPlayLocal) {
        localUrl?.let { url -> { onPlayLocal(url, item) } }
    }

    val rowModifier = if (modifier == Modifier) FAVORITE_CARD_BASE_MODIFIER else modifier.then(FAVORITE_CARD_BASE_MODIFIER)

    Box(
        modifier = rowModifier
    ) {
        when {
            // Скачано: показываем постер, по тапу — локальное воспроизведение полного файла.
            localUrl != null && onPlayLocalThis != null -> {
                UrlImage(
                    posterUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onPlayLocalThis)
                )
                // Значок «скачано» (как в R — IconSave18).
                Row(
                    modifier = DOWNLOAD_ICON_PADDING_MODIFIER
                ) {
                    IconSave18()
                }
            }

            else -> UrlVideoImageAndLongClickX(
                item,
                onLongClick = onOpenThisVideo,
                onDoubleClick = onOpenThisVideo,
            )
        }

        Row(Modifier.align(ALIGN_TOP_END)) {
            FavoriteActionsExpandMenu(
                onDelete = onDeleteThis,
                onDownload = onDownloadThis,
                onSaveToGallery = onSaveToGalleryThis,
            )
        }

        Row(Modifier.align(ALIGN_BOTTOM_END).then(DURATION_END_PADDING_MODIFIER)) { DurationOverlay(item.duration) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteActionsExpandMenu(
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onSaveToGallery: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    val onExpandedChange = remember { { isExpanded: Boolean -> expanded = isExpanded } }
    val onDismissMenu = remember { { expanded = false } }
    val handleDownload = remember(onDownload) {
        {
            onDownload()
            expanded = false
        }
    }
    val handleSaveToGallery = remember(onSaveToGallery) {
        {
            onSaveToGallery()
            expanded = false
        }
    }
    val handleDelete = remember(onDelete) {
        {
            onDelete()
            expanded = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        IconButton(
            modifier = ACTION_BUTTON_SIZE_MODIFIER
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
            onClick = NO_OP_CLICK
        ) {
            Icon(
                ICON_MORE_VERT,
                contentDescription = TEXT_ACTIONS,
                tint = COLOR_BLACK,
                modifier = ACTION_ICON_SHADOW_MODIFIER
            )
            Icon(
                ICON_MORE_VERT,
                contentDescription = null,
                tint = COLOR_WHITE,
                modifier = ACTION_ICON_SIZE_MODIFIER
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissMenu,
            modifier = MENU_WIDTH_MODIFIER,
            containerColor = Theme.ExpandMenu.backgroundColor
        ) {
            ExpandMenuActionItem(ICON_DOWNLOAD, TEXT_DOWNLOAD, onClick = handleDownload)
            ExpandMenuActionItem(ICON_SAVE_ALT, TEXT_IN_GALLERY, onClick = handleSaveToGallery)
            ExpandMenuActionItem(ICON_DELETE, TEXT_DELETE, onClick = handleDelete)
        }
    }
}

/** Продолжительность видео в правом верхнем углу с «тенью» (как в оригинале). */
@Composable
private fun DurationOverlay(duration: String) {
    val text = remember(duration) { duration.trim().removeSuffix(".") }
    if (text.isEmpty()) return
    Box(modifier = Modifier) {
        Text(
            text = text,
            modifier = DURATION_SHADOW_MODIFIER,
            textAlign = TEXT_ALIGN_RIGHT,
            fontSize = DURATION_FONT_SIZE,
            color = COLOR_BLACK
        )
        Text(
            text = text,
            modifier = DURATION_TEXT_MODIFIER,
            textAlign = TEXT_ALIGN_RIGHT,
            fontSize = DURATION_FONT_SIZE,
            color = COLOR_WHITE
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun DurationOverlayPreview() {
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 32.dp)
            .background(Color(0xFF3A3A3A))
    ) {
        DurationOverlay("12:34")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun ScreenFavoritesPreview() {
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
            ),
            ItemsX(
                id = 2L,
                title = "Another sample",
                duration = "03:10",
                views = "500K",
                channel = "Channel2",
                href = "/video2",
                nameProfile = "Channel2",
                linkProfile = "/channel2",
            ),
        ),
        localUrlOf = { null },
        posterUrlOf = { it.previewImage },
        onDelete = {},
        onDownload = {},
        onPlayLocal = { _, _ -> },
        onOpenVideo = {},
    )
}
