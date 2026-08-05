package com.client.xvideos.x.screens.dashboards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.icons.IconFavorite18
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.urlStart
import com.client.xvideos.x.feature.country.CountryState
import com.client.xvideos.x.feature.net.readHtmlFromURLWebView
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parseSiteCountryFlag
import com.client.xvideos.x.parcer.parserListVideo
import com.client.xvideos.x.screens.ui.expandMenu.X_DashboardExpandMenu
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private suspend fun openNew(numberScreen: Int = 0): SnapshotStateList<ItemsX> {
    val currentNumberScreen = numberScreen.coerceIn(0, 19999)
    val url = urlStart + if (currentNumberScreen == 0) "" else "/new/${currentNumberScreen}"
    Timber.i("!!! openNew numberScreen:$numberScreen url:$url")
    val html = readHtmlFromURLWebView(url)
    // X4: страну выставляет вызывающий, парсер остаётся чистым.
    parseSiteCountryFlag(html)?.let { CountryState.current = it }
    return parserListVideo(html).toMutableStateList()
}


/**
 *
 * ![Логотип Markdown](https://ah-img.luscious.net/Joking42/499900/sample_3941cb87cea03_01J9ZXQ9XTDKY6PQ01ZRWF1FFZ.1680x0.jpg)
 *
 *
 */
@Composable
fun DashboardsPaginatedListScreen(
    pageIndex: Int,
    openVideoPlayer: (String) -> Unit,

    isFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit = {},
) {

    val l = remember { mutableStateListOf<ItemsX>() }

    LaunchedEffect(key1 = pageIndex, key2 = CountryState.current) {
        // Список очищаем только когда новая страница уже загружена: раньше
        // clear() стоял перед сетевым вызовом, и всё время запроса лента была пустой.
        val items = withContext(Dispatchers.IO) {
            openNew(pageIndex).filter { !it.href.contains("THUMBNUM") }
        }
        l.replaceWith(items)
    }


    if (l.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){ CircularProgressIndicator(modifier = Modifier.size(40.dp)) }
    } else {
        DashboardsPaginatedListContent(
            items = l.toImmutableList(),
            isFavorite = isFavorite,
            onFavoriteAdd = onFavoriteAdd,
            onFavoriteRemove = onFavoriteRemove,
            onDownload = onDownload,
            onSaveToGallery = onSaveToGallery,
            openVideoPlayer = openVideoPlayer
        )
    }
}

@Composable
fun DashboardsPaginatedListContent(
    items: ImmutableList<ItemsX>,
    isFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    openVideoPlayer: (String) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
    )
    {
        itemsIndexed(items, key = { index, it -> "${it.id}_$index" })
        { index, cell ->

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(352f / 198f)
                    .padding(1.dp)
                    .background(Color.DarkGray)
            )
            {
                //Отобразить карточку картинка видео
                UrlVideoImageAndLongClickX(
                    cell,
                    onLongClick = {
                        //Открыть экран плеера
                        openVideoPlayer(urlStart + cell.href)
                    },
                    onDoubleClick = {
                        openVideoPlayer(urlStart + cell.href)
                    }
                )
                {

                    Box(modifier = Modifier.fillMaxSize()) {
                        val offsetY = (-3).dp

                        //Продолжительность видео
                        Text(
                            text = cell.duration.dropLast(1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(0.5.dp, offsetY + 0.5.dp),
                            textAlign = TextAlign.Right,
                            fontSize = 14.sp,
                            color = Color.Black
                        )

                        Text(
                            text = cell.duration.dropLast(1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(0.dp, offsetY),
                            textAlign = TextAlign.Right,
                            fontSize = 14.sp,
                            color = Color.White
                        )

                    }


                    //Название канала
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(Color(0x60000000)), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cell.channel,
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Row(modifier = Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.End) {
                        //if (vm.saved.favorites.contains(cell.id)) {
                        if (isFavorite(cell.id))
                            //Индикатор что видео в фаворитах
                            Box(modifier = Modifier) { IconFavorite18(Modifier.padding(bottom = 6.dp, end = 6.dp)) }
                        }
                    }

                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    X_DashboardExpandMenu(
                        isFavorite = isFavorite(cell.id),//vm.isFavorite(cell.id),
                        onFavoriteAdd = { onFavoriteAdd(cell) },
                        onFavoriteRemove = { onFavoriteRemove(cell) },
                        onDownload = { onDownload(cell) },
                        onSaveToGallery = { onSaveToGallery(cell) },
                    )
                }

                }
            }
        }

}


@Preview(showBackground = true)
@Composable
private fun DashboardsPaginatedListScreenPreview() {
    XvideosTheme {
        DashboardsPaginatedListContent(
            items = listOf(
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
            ).toImmutableList(),
            isFavorite = { it == 1L },
            onFavoriteAdd = {},
            onFavoriteRemove = {},
            onDownload = {},
            openVideoPlayer = {}
        )
    }
}
