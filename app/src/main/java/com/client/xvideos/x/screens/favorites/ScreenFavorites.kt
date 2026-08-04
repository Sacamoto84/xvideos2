package com.client.xvideos.x.screens.favorites

import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.client.xvideos.common.urlVideoImage.UrlVideoImageAndLongClickX
import com.client.xvideos.x.model.ItemsX
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
            posterUrlOf = { item ->
                if (item.id in downloadedIds) (vm.saved.downloads.localPosterPath(item.id) ?: item.previewImage)
                else item.previewImage
            },
            onProfile = { navigator.push(ScreenProfile()) },
            onDelete = { vm.removeFavorite(it) },
            onDownload = { vm.download(it) },
            onSaveToGallery = { vm.saveToGallery(it) },
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
    posterUrlOf: (ItemsX) -> String,
    onProfile: () -> Unit,
    onDelete: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onPlayLocal: (String) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit = {},
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

    Scaffold(modifier = Modifier.fillMaxSize(), backgroundColor = Theme.L.grey6, topBar = {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Theme.L.grey6),
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
                        contentDescription = "Профиль",
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
            // padding от Scaffold — высота topBar. Без него первая строка сетки
            // уезжала под заголовок «Избранное».
            modifier = Modifier.padding(padding)
        ) {
            items(favorites) { item ->
                FavoriteRow(
                    item = item,
                    localUrl = localUrlOf(item),
                    posterUrl = posterUrlOf(item),
                    onDelete = { pendingDelete = item },
                    onDownload = { onDownload(item) },
                    onSaveToGallery = { onSaveToGallery(item) },
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
    posterUrl: String,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onPlayLocal: (String) -> Unit,
    onSaveToGallery: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .padding(horizontal = 1.dp)
            //.clip(RoundedCornerShape(8.dp))
            //.border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
            .aspectRatio(352f / 198f)
            .background(Color.DarkGray)

    ) {

            when {

                // Скачано: показываем постер, по тапу — локальное воспроизведение полного файла.
                localUrl != null -> {
                    UrlImage(
                        posterUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onPlayLocal(localUrl) }
                    )
                    // Значок «скачано» (как в R — IconSave18).
                    Row(
                        modifier = Modifier
                        //.align(Alignment.TopStart)
                        //.padding(6.dp)
                            //.background(Color(0x99000000), RoundedCornerShape(50))
                        .padding(4.dp)
                    ) {
                        IconSave18()
                    }
                }

                // В preview видео-компонент не поднимаем (нет контекста/сети) — только оверлей.
                //LocalInspectionMode.current -> DurationOverlay(item.duration)
                // Не скачано: обычный сетевой превью-компонент.
                else -> UrlVideoImageAndLongClickX(item, onLongClick = {}, onDoubleClick = {})
            }


        Row(Modifier.align(Alignment.TopEnd)) {
            FavoriteActionsExpandMenu(
                onDelete = onDelete,
                onDownload = onDownload,
                onSaveToGallery = onSaveToGallery,
            )
        }

        Row(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp)) { DurationOverlay(item.duration) }



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
                contentDescription = "Действия",
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .offset(0.5.dp, 0.5.dp)
            )
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Min),
            containerColor = Theme.ExpandMenu.backgroundColor
        ) {
            ExpandMenuActionItem(Icons.Filled.ArrowCircleDown, "Скачать") {
                onDownload()
                expanded = false
            }

            ExpandMenuActionItem(Icons.Filled.SaveAlt, "В галерею") {
                onSaveToGallery()
                expanded = false
            }

            ExpandMenuActionItem(Icons.Filled.Delete, "Удалить") {
                onDelete()
                expanded = false
            }
        }
    }
}

/** Продолжительность видео в правом верхнем углу с «тенью» (как в оригинале). */
@Composable
private fun DurationOverlay(duration: String) {
    val offsetY = (-3).dp
    val text = duration.dropLast(1)
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
        onProfile = {},
        onDelete = {},
        onDownload = {},
        onPlayLocal = {},
    )
}
