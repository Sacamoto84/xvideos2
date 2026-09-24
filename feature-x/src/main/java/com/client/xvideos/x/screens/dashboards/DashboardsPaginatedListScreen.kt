package com.client.xvideos.x.screens.dashboards

import androidx.compose.foundation.ExperimentalFoundationApi
import com.client.xvideos.common.ui.lazy.viewportFractionCacheWindow
import androidx.compose.foundation.lazy.grid.LazyGridState
import com.client.xvideos.common.util.getTopInsetDp
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.icons.IconFavorite18
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private val ERROR_BADGE_SHAPE = RoundedCornerShape(8.dp)
private val ERROR_BADGE_BG = Color(0xD9212121)
private const val GRID_COLUMNS = 2
private val SPINNER_SIZE = 40.dp
private val RETRY_BUTTON_HEIGHT = 32.dp
private val ERROR_SPACER_HEIGHT = 12.dp
private val ERROR_BADGE_PADDING = 8.dp
private val ERROR_BADGE_CONTENT_HORIZONTAL = 12.dp
private val ERROR_BADGE_CONTENT_VERTICAL = 6.dp
private val ERROR_TEXT_FONT_SIZE = 12.sp

private const val TEXT_LOAD_ERROR = "Не удалось загрузить страницу"
private const val TEXT_UPDATE_ERROR = "Не удалось обновить страницу"
private const val TEXT_RETRY = "Повторить"
private const val CONTENT_TYPE_DASHBOARD_CELL = "dashboard_cell"

private const val DASHBOARD_CARD_ASPECT_RATIO = 352f / 198f
private val CHANNEL_BADGE_BG = Color(0x60000000)

internal fun buildDashboardUrl(numberScreen: Int): String {
    val currentNumberScreen = numberScreen.coerceIn(0, 19999)
    val raw = urlStart + if (currentNumberScreen == 0) "" else "/new/$currentNumberScreen"
    return normalizeXUrl(raw)
}

private suspend fun openNew(numberScreen: Int = 0): Pair<String?, List<ItemsX>> {
    val url = buildDashboardUrl(numberScreen)
    Timber.d("openNew numberScreen:$numberScreen url:$url")
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
 * Экран страницы пагинированного списка видео дашборда (Best, Top Rated, Newest).
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("DEPRECATION")
@Composable
fun DashboardsPaginatedListScreen(
    pageIndex: Int,
    openVideoPlayer: (ItemsX) -> Unit,

    isFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit = {},
) {

    var videoItems by remember(pageIndex) { mutableStateOf<ImmutableList<ItemsX>>(persistentListOf()) }
    var hasError by remember(pageIndex) { mutableStateOf(false) }
    var retryTrigger by remember(pageIndex) { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState(cacheWindow = viewportFractionCacheWindow())

    LaunchedEffect(key1 = pageIndex, key2 = CountryState.userSelectionEpoch, key3 = retryTrigger) {
        // Список очищаем только когда новая страница уже загружена: раньше
        // clear() стоял перед сетевым вызовом, и всё время запроса лента была пустой.
        hasError = false
        try {
            val (flag, items) = openNew(pageIndex)
            flag?.let { CountryState.updateCurrent(it) }
            if (items.isEmpty()) {
                hasError = true
            } else {
                videoItems = items.toImmutableList()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "DashboardsPaginatedListScreen: ошибка загрузки pageIndex=$pageIndex")
            hasError = true
        }
    }


    val onRetry: () -> Unit = remember(pageIndex) { { retryTrigger++ } }

    if (videoItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(TEXT_LOAD_ERROR, color = Color.Gray)
                    Spacer(modifier = Modifier.height(ERROR_SPACER_HEIGHT))
                    Button(onClick = onRetry) {
                        Text(TEXT_RETRY)
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(SPINNER_SIZE))
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            DashboardsPaginatedListContent(
                items = videoItems,
                isFavorite = isFavorite,
                onFavoriteAdd = onFavoriteAdd,
                onFavoriteRemove = onFavoriteRemove,
                onDownload = onDownload,
                onSaveToGallery = onSaveToGallery,
                openVideoPlayer = openVideoPlayer,
                gridState = gridState,
            )
            val topCutout = getTopInsetDp()
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = topCutout + ERROR_BADGE_PADDING, start = ERROR_BADGE_PADDING, end = ERROR_BADGE_PADDING, bottom = ERROR_BADGE_PADDING)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ERROR_BADGE_BG, ERROR_BADGE_SHAPE)
                            .padding(horizontal = ERROR_BADGE_CONTENT_HORIZONTAL, vertical = ERROR_BADGE_CONTENT_VERTICAL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = TEXT_UPDATE_ERROR,
                            color = Color.White,
                            fontSize = ERROR_TEXT_FONT_SIZE,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.height(RETRY_BUTTON_HEIGHT),
                            contentPadding = PaddingValues(horizontal = ERROR_BADGE_CONTENT_HORIZONTAL, vertical = 0.dp)
                        ) {
                            Text(TEXT_RETRY, fontSize = ERROR_TEXT_FONT_SIZE)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("DEPRECATION")
@Composable
fun DashboardsPaginatedListContent(
    items: ImmutableList<ItemsX>,
    isFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    openVideoPlayer: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit = {},
    gridState: LazyGridState = rememberLazyGridState(cacheWindow = viewportFractionCacheWindow()),
) {
    val topCutout = getTopInsetDp()
    val contentPadding = remember(topCutout) { PaddingValues(top = topCutout) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = contentPadding,
    ) {
        itemsIndexed(
            items = items,
            key = { index, cell -> "${cell.id}#$index" },
            contentType = { _, _ -> CONTENT_TYPE_DASHBOARD_CELL }
        ) { _, cell ->
            DashboardGridCell(
                cell = cell,
                isFavorite = isFavorite(cell.id),
                openVideoPlayer = openVideoPlayer,
                onFavoriteAdd = onFavoriteAdd,
                onFavoriteRemove = onFavoriteRemove,
                onDownload = onDownload,
                onSaveToGallery = onSaveToGallery,
            )
        }
    }
}

@Composable
private fun DashboardGridCell(
    cell: ItemsX,
    isFavorite: Boolean,
    openVideoPlayer: (ItemsX) -> Unit,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(cell, openVideoPlayer) { { openVideoPlayer(cell) } }
    val handleFavoriteAdd = remember(cell, onFavoriteAdd) { { onFavoriteAdd(cell) } }
    val handleFavoriteRemove = remember(cell, onFavoriteRemove) { { onFavoriteRemove(cell) } }
    val handleDownload = remember(cell, onDownload) { { onDownload(cell) } }
    val handleSaveToGallery = remember(cell, onSaveToGallery) { { onSaveToGallery(cell) } }
    val durationText = remember(cell.duration) { cell.duration.trim().removeSuffix(".") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(DASHBOARD_CARD_ASPECT_RATIO)
            .padding(1.dp)
            .background(Color.DarkGray)
    ) {
        UrlVideoImageAndLongClickX(
            cell,
            onLongClick = handleOpen,
            onDoubleClick = handleOpen,
        ) {
            if (durationText.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val offsetY = (-3).dp

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

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(CHANNEL_BADGE_BG),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cell.channel,
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Row(modifier = Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.End) {
                if (isFavorite) {
                    Box(modifier = Modifier) { IconFavorite18(Modifier.padding(bottom = 6.dp, end = 6.dp)) }
                }
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                X_DashboardExpandMenu(
                    isFavorite = isFavorite,
                    onFavoriteAdd = handleFavoriteAdd,
                    onFavoriteRemove = handleFavoriteRemove,
                    onDownload = handleDownload,
                    onSaveToGallery = handleSaveToGallery,
                )
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
            isFavorite = { id -> id == 1L },
            onFavoriteAdd = {},
            onFavoriteRemove = {},
            onDownload = {},
            openVideoPlayer = {}
        )
    }
}
