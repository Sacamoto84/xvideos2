package com.client.xvideos.x.screens.saved

import com.client.xvideos.common.theme.Theme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.client.xvideos.common.theme.LavenderDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import com.client.xvideos.common.util.getTopInsetDp
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import androidx.compose.material.icons.filled.Share
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.export.XExporter
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.snackbar.SnackBar
import java.io.File

private const val VIDEO_ASPECT_RATIO = 352f / 198f
private const val TEXT_SAVED_TITLE = "Сохранённое"
private const val TEXT_EMPTY = "Пусто"
private const val TEXT_DELETE_CONFIRM_TITLE = "Удалить из сохранённого?"
private const val TEXT_DELETE = "Удалить"
private const val CD_P2P = "P2P"
private const val CD_DELETE = "Удалить"
private const val CONTENT_TYPE_HEADER = "header"
private const val CONTENT_TYPE_SAVED_ROW = "saved_row"

private val SAVED_DIVIDER_COLOR = Color(0xFF9E9E9E)
private val SHARE_ICON_SIZE = 26.dp
private val DELETE_ICON_SIZE = 28.dp
private val DIALOG_PREVIEW_WIDTH = 160.dp
private val DIALOG_IMAGE_SHAPE = RoundedCornerShape(8.dp)

private val SAVED_ROW_COLUMN_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(vertical = 2.dp)
private val SAVED_ROW_MEDIA_BOX_BASE = Modifier
    .fillMaxWidth()
    .aspectRatio(VIDEO_ASPECT_RATIO)
    .background(Color.DarkGray)
private val SAVED_ROW_ACTIONS_ROW_MODIFIER = Modifier
    .fillMaxWidth()
    .background(Theme.L.grey6)
private val FILL_MAX_SIZE_MODIFIER = Modifier.fillMaxSize()
private val SAVED_ROOT_BASE_MODIFIER = Modifier
    .fillMaxSize()
    .background(Theme.L.grey6)
private val SHARE_ICON_MODIFIER = Modifier.size(SHARE_ICON_SIZE)
private val DELETE_ICON_MODIFIER = Modifier.size(DELETE_ICON_SIZE)
private val SAVED_HEADER_ROW_MODIFIER = Modifier
    .fillMaxWidth()
    .background(Theme.L.grey6)
private val SAVED_HEADER_TITLE_MODIFIER = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
private val DIALOG_IMAGE_MODIFIER = Modifier
    .width(DIALOG_PREVIEW_WIDTH)
    .aspectRatio(VIDEO_ASPECT_RATIO)
    .clip(DIALOG_IMAGE_SHAPE)
private val SAVED_ROW_CONTENT_TYPE = { _: ItemsX -> CONTENT_TYPE_SAVED_ROW }

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
            title = TEXT_DELETE_CONFIRM_TITLE,
            imageUrl = dialogImageUrl,
            onConfirm = onConfirmItem,
            onDismiss = onDismissDelete,
        )
    }

    val topCutout = getTopInsetDp()
    val rootModifier = if (modifier == Modifier) {
        SAVED_ROOT_BASE_MODIFIER
    } else {
        modifier.then(SAVED_ROOT_BASE_MODIFIER)
    }

    Column(
        modifier = rootModifier
    ) {
        if (list.isEmpty()) {
            Column(modifier = FILL_MAX_SIZE_MODIFIER) {
                SavedHeader(topCutout = topCutout)
                Box(modifier = FILL_MAX_SIZE_MODIFIER, contentAlignment = Alignment.Center) {
                    Text(TEXT_EMPTY, color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = FILL_MAX_SIZE_MODIFIER
            ) {
                item(key = CONTENT_TYPE_HEADER, contentType = CONTENT_TYPE_HEADER) {
                    SavedHeader(topCutout = topCutout)
                }
                items(
                    items = list,
                    key = { it.id },
                    contentType = SAVED_ROW_CONTENT_TYPE
                ) { item ->
                    val posterUrl = remember(item.id, item.previewImage, saved.downloads) {
                        saved.downloads.localPosterPath(item.id) ?: item.previewImage
                    }
                    SavedRow(
                        item = item,
                        posterUrl = posterUrl,
                        onPlay = onPlayItem,
                        onDelete = onDeleteItem,
                        onShareP2p = onShareP2pItem,
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedHeader(
    modifier: Modifier = Modifier,
    topCutout: Dp = 0.dp,
) {
    val headerModifier = if (modifier == Modifier) {
        Modifier
            .fillMaxWidth()
            .padding(top = topCutout)
    } else {
        modifier
            .fillMaxWidth()
            .padding(top = topCutout)
    }
    Column(
        modifier = headerModifier
    ) {
        Row(
            modifier = SAVED_HEADER_ROW_MODIFIER,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                TEXT_SAVED_TITLE,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = SAVED_HEADER_TITLE_MODIFIER
            )
        }
        HorizontalDivider(color = SAVED_DIVIDER_COLOR)
    }
}

@Composable
private fun SavedRow(
    item: ItemsX,
    posterUrl: String,
    onPlay: (ItemsX) -> Unit,
    onDelete: (ItemsX) -> Unit,
    onShareP2p: (ItemsX) -> Unit
) {
    val handlePlay = remember(item, onPlay) { { onPlay(item) } }
    val handleShareP2p = remember(item, onShareP2p) { { onShareP2p(item) } }
    val handleDelete = remember(item, onDelete) { { onDelete(item) } }

    Column(
        modifier = SAVED_ROW_COLUMN_MODIFIER
    ) {
        Box(
            modifier = SAVED_ROW_MEDIA_BOX_BASE
                .clickable(onClick = handlePlay)
        ) {
            UrlImage(url = posterUrl, modifier = FILL_MAX_SIZE_MODIFIER)

            // Продолжительность видео в правом верхнем углу.
            Text(
                text = item.duration,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                textAlign = TextAlign.Right,
                fontSize = 14.sp,
                color = Color.White
            )
        }

        Row(
            modifier = SAVED_ROW_ACTIONS_ROW_MODIFIER,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            IconButton(onClick = handleShareP2p) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = CD_P2P,
                    tint = Color.Gray,
                    modifier = SHARE_ICON_MODIFIER
                )
            }

            IconButton(onClick = handleDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = CD_DELETE,
                    tint = Color.Gray,
                    modifier = DELETE_ICON_MODIFIER
                )
            }
        }
    }
}

/** Универсальный диалог подтверждения удаления (тёмный стиль под фон L). */
@Composable
fun ConfirmDeleteVideoDialog(
    title: String,
    imageUrl: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LavenderDialog(
        title = title,
        onDismiss = onDismiss,
        icon = {
            UrlImage(
                url = imageUrl,
                modifier = DIALOG_IMAGE_MODIFIER
            )
        },
        confirmText = TEXT_DELETE,
        onConfirm = onConfirm,
        destructive = true,
    )
}
