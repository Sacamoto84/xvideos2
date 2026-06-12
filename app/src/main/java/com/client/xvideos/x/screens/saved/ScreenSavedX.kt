package com.client.xvideos.x.screens.saved

import com.client.xvideos.common.theme.Theme

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import androidx.compose.material.icons.filled.Share
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.p2p.P2pExportBundle
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.export.XExporter
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.snackbar.SnackBar
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
    val list = saved.downloads.list.collectAsStateWithLifecycle().value

    var pendingDelete by remember { mutableStateOf<ItemsX?>(null) }
    pendingDelete?.let { item ->
        ConfirmDeleteVideoDialog(
            title = "Удалить из сохранённого?",
            imageUrl = saved.downloads.localPosterPath(item.id) ?: item.previewImage,
            onConfirm = {
                saved.downloads.delete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.L.grey6)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().background(Theme.L.grey6),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Сохранённое",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }
        HorizontalDivider(color = Color(0xFF9E9E9E))

        if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Пусто", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn {
                items(list) { item ->
                    val posterUrl = remember(item.id) {
                        saved.downloads.localPosterPath(item.id) ?: item.previewImage
                    }
                    SavedRow(
                        item = item,
                        posterUrl = posterUrl,
                        onPlay = { navigator.push(ScreenX_LocalVideoPlayer(saved.downloads.localUrl(item.id))) },
                        onDelete = { pendingDelete = item },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedRow(item: ItemsX, posterUrl: String, onPlay: () -> Unit, onDelete: () -> Unit) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(352f / 198f)
                .background(Color.DarkGray)
                .clickable { onPlay() }
        ) {
            UrlImage(url = posterUrl, modifier = Modifier.fillMaxSize())

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
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.L.grey6),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 2,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )

            IconButton(onClick = {
                val bundle = XExporter.export(File(AppPath.x_cache_download), item.id)
                if (bundle == null) {
                    SnackBar.error("Нет скачанного видео для P2P")
                } else {
                    navigator.push(ScreenP2pSend(P2pSendSource.Ready(bundle)))
                }
            }) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "P2P",
                    tint = Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Удалить",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            UrlImage(
                url = imageUrl,
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(352f / 198f)
                    .clip(RoundedCornerShape(8.dp))
            )
        },
        title = { Text(title, color = Color.White) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить", color = Color(0xFFFF6B6B))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFFCCCCCC))
            }
        },
        containerColor = Color(0xFF2E2E2E),
    )
}
