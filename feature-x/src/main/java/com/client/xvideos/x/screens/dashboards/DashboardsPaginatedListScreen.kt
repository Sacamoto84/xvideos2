package com.client.xvideos.x.screens.dashboards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.ui.lazy.viewportFractionCacheWindow
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.feature.country.CountryState
import com.client.xvideos.x.feature.net.readHtmlFromURLWebView
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.parcer.parseSiteCountryFlag
import com.client.xvideos.x.parcer.parserListVideo
import com.client.xvideos.x.screens.dashboards.molecule.DashboardsPaginatedListContent
import com.client.xvideos.x.urlStart
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

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
@Composable
fun DashboardsPaginatedListScreen(
    pageIndex: Int,
    openVideoPlayer: (ItemsX) -> Unit,
    isFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
    onSaveToGallery: (ItemsX) -> Unit = {},
) {
    var videoItems by remember(pageIndex) { mutableStateOf<ImmutableList<ItemsX>>(persistentListOf()) }
    var hasError by remember(pageIndex) { mutableStateOf(false) }
    var retryTrigger by remember(pageIndex) { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState(cacheWindow = viewportFractionCacheWindow())

    LaunchedEffect(key1 = pageIndex, key2 = CountryState.userSelectionEpoch, key3 = retryTrigger) {
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
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Не удалось загрузить страницу", color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRetry) {
                        Text("Повторить")
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
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
                        .padding(top = topCutout + 8.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
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
                            text = "Не удалось обновить страницу",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onRetry,
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

@Preview(showBackground = true)
@Composable
private fun DashboardsPaginatedListScreenPreview() {
    DashboardsPaginatedListContent(
        items = persistentListOf(
            ItemsX(
                id = 1L,
                title = "Sample video",
                duration = "12:34",
                views = "1.2M",
                channel = "Old4k",
            )
        ),
        isFavorite = { false },
        onFavoriteAdd = {},
        onFavoriteRemove = {},
        onDownload = {},
        openVideoPlayer = {}
    )
}
