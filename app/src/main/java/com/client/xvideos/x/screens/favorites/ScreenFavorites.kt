package com.client.xvideos.x.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.common.urlVideoImage.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.profile.ScreenProfile
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import com.composables.core.HorizontalSeparator

class ScreenFavorites() : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenFavoritesSM = getScreenModel()

        // Множество id скачанных видео (реактивно) — для значка и локального воспроизведения.
        val downloaded = vm.saved.downloads.list.collectAsStateWithLifecycle().value
        val downloadedIds = remember(downloaded) { downloaded.map { it.id }.toSet() }

        FavoritesContent(
            favorites = vm.favorites,
            localUrlOf = { item ->
                if (item.id in downloadedIds) vm.saved.downloads.localUrl(item.id) else null
            },
            onProfile = { navigator.push(ScreenProfile()) },
            onDelete = { vm.removeFavorite(it) },
            onDownload = { vm.download(it) },
            onPlayLocal = { url -> navigator.push(ScreenX_LocalVideoPlayer(url)) },
        )
    }
}

/**
 * Stateless-тело экрана «Избранное» — пригодно для [Preview] (без Hilt/Navigator).
 *
 * @param localUrlOf для скачанного видео возвращает `file://`-URL локального файла, иначе null.
 * @param onPlayLocal открыть локальное воспроизведение по `file://`-URL.
 */
@Composable
private fun FavoritesContent(
    favorites: List<ItemsX>,
    localUrlOf: (ItemsX) -> String?,
    onProfile: () -> Unit,
    onDelete: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onPlayLocal: (String) -> Unit,
) {
    // Подтверждение удаления из избранного (диалог).
    var pendingDelete by remember { mutableStateOf<ItemsX?>(null) }
    pendingDelete?.let { item ->
        ConfirmDeleteFavoriteDialog(
            item = item,
            onConfirm = {
                onDelete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize(), backgroundColor = ThemeL.grey6, topBar = {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeL.grey6),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Избранное",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )

                IconButton(onClick = onProfile) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(32.dp)
                    )
                }
            }
            HorizontalSeparator(color = Color(0xFF9E9E9E))
        }
    }) { padding ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding)
        ) {
            items(favorites) { item ->
                FavoriteRow(
                    item = item,
                    localUrl = localUrlOf(item),
                    onDelete = { pendingDelete = item },
                    onDownload = { onDownload(item) },
                    onPlayLocal = onPlayLocal,
                )
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    item: ItemsX,
    localUrl: String?,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onPlayLocal: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(352f / 198f)
                .background(Color.DarkGray)
        ) {
            when {
                // Скачано: показываем постер, по тапу — локальное воспроизведение полного файла.
                localUrl != null -> {
                    UrlImage(
                        item.previewImage,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onPlayLocal(localUrl) }
                    )
                    // Значок «скачано» (как в R — IconSave18).
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Color(0x99000000), RoundedCornerShape(50))
                            .padding(4.dp)
                    ) {
                        IconSave18()
                    }
                }
                // В preview видео-компонент не поднимаем (нет контекста/сети) — только оверлей.
                LocalInspectionMode.current -> DurationOverlay(item.duration)
                // Не скачано: обычный сетевой превью-компонент.
                else -> UrlVideoImageAndLongClickX(item, onLongClick = {}, onDoubleClick = {})
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeL.grey6), verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Filled.ArrowCircleDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }


            Box(modifier = Modifier.padding(end = 8.dp)) {
                DurationOverlay(item.duration)
            }

        }
    }
}

/** Продолжительность видео в правом верхнем углу с «тенью» (как в оригинале). */
@Composable
private fun DurationOverlay(duration: String) {
    val offsetY = (-3).dp
    val text = duration.dropLast(1)
    Box(modifier = Modifier.fillMaxSize()) {
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
        onProfile = {},
        onDelete = {},
        onDownload = {},
        onPlayLocal = {},
    )
}
