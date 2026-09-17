package com.client.xvideos.x.screens.dashboards

import androidx.compose.foundation.ExperimentalFoundationApi
import com.client.xvideos.common.ui.lazy.viewportFractionCacheWindow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.icons.IconFavorite18
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.x.urlStart
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.feature.country.CountryState
import com.client.xvideos.x.feature.net.readHtmlFromURLWebView
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parcer.parseSiteCountryFlag
import com.client.xvideos.x.parcer.parserListVideo
import com.client.xvideos.x.screens.ui.expandMenu.X_DashboardExpandMenu
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private suspend fun openNew(numberScreen: Int = 0): Pair<String?, List<ItemsX>> {
    val currentNumberScreen = numberScreen.coerceIn(0, 19999)
    val url = urlStart + if (currentNumberScreen == 0) "" else "/new/${currentNumberScreen}"
    Timber.i("!!! openNew numberScreen:$numberScreen url:$url")
    val html = readHtmlFromURLWebView(url)
    return withContext(Dispatchers.Default) {
        val document = org.jsoup.Jsoup.parse(html)
        val flag = parseSiteCountryFlag(document)
        val items = parserListVideo(document)
            .filter { !it.href.contains("THUMBNUM") }
            .distinctBy { it.id }
        flag to items
    }
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

    val l = remember(pageIndex) { mutableStateListOf<ItemsX>() }
    var hasError by remember(pageIndex) { mutableStateOf(false) }
    var retryTrigger by remember(pageIndex) { mutableIntStateOf(0) }

    LaunchedEffect(key1 = pageIndex, key2 = CountryState.userSelectionEpoch, key3 = retryTrigger) {
        // Список очищаем только когда новая страница уже загружена: раньше
        // clear() стоял перед сетевым вызовом, и всё время запроса лента была пустой.
        hasError = false
        try {
            val (flag, items) = openNew(pageIndex)
            flag?.let { CountryState.updateCurrent(it) }
            if (items.isEmpty()) {
                hasError = true
                SnackBar.error("Не удалось загрузить видео")
            } else {
                l.replaceWith(items)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "DashboardsPaginatedListScreen: ошибка загрузки pageIndex=$pageIndex")
            hasError = true
            SnackBar.error("Ошибка загрузки видео")
        }
    }


    if (l.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ошибка загрузки видео", color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { retryTrigger++ }) {
                        Text("Повторить")
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            DashboardsPaginatedListContent(
                items = l.toImmutableList(),
                isFavorite = isFavorite,
                onFavoriteAdd = onFavoriteAdd,
                onFavoriteRemove = onFavoriteRemove,
                onDownload = onDownload,
                onSaveToGallery = onSaveToGallery,
                openVideoPlayer = openVideoPlayer
            )
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xD9212121), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Не удалось обновить видео",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = { retryTrigger++ },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Повторить", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
        // Окно предзагрузки долей вьюпорта: каждая карточка — живое видео-превью,
        // и заднее окно не даёт пересоздавать плееры при прокрутке вверх.
        state = rememberLazyGridState(cacheWindow = viewportFractionCacheWindow()),
    )
    {
        items(items, key = { it.id }) { cell ->

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
                        openVideoPlayer(normalizeXUrl(cell.href))
                    },
                    onDoubleClick = {
                        openVideoPlayer(normalizeXUrl(cell.href))
                    }
                )
                {
                    val durationText = cell.duration.trim().removeSuffix(".")
                    if (durationText.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val offsetY = (-3).dp

                            //Продолжительность видео
                            Text(
                                text = durationText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(0.5.dp, offsetY + 0.5.dp),
                                textAlign = TextAlign.Right,
                                fontSize = 14.sp,
                                color = Color.Black
                            )

                            Text(
                                text = durationText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(0.dp, offsetY),
                                textAlign = TextAlign.Right,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
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
