package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.ui.atom.TopLoadingBar
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.model.ChannelSortOrder
import com.client.xvideos.x.model.ItemsX

@Suppress("LongParameterList")
@Composable
fun ChannelVideosPager(
    pagerState: PagerState,
    pagesCache: Map<Int, List<ItemsX>>,
    loadingPages: Set<Int>,
    errorPages: Map<Int, String>,
    currentSort: ChannelSortOrder,
    selectedModel: ChannelModelFilterItem?,
    isModel: Boolean,
    getGridState: (Int) -> LazyGridState,
    onLoadPage: (Int) -> Unit,
    onRetryPage: (Int) -> Unit,
    onOpenVideo: (ItemsX) -> Unit,
    isFavorite: (Long) -> Boolean,
    isDownloaded: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
    headerScrollModifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { pageIndex -> pageIndex }
    ) { page ->
        val pageVideos = pagesCache[page]
        val isPageLoading = page in loadingPages
        val pageError = errorPages[page]
        val gridState = getGridState(page)

        LaunchedEffect(page, currentSort, selectedModel) {
            if (pageVideos == null && !isPageLoading && pageError == null) {
                onLoadPage(page)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isPageLoading && pageVideos == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(headerScrollModifier),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFDE2600),
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else if (pageError != null && pageVideos == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(headerScrollModifier)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pageError,
                            color = Color(0xFFCCCCCC),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onRetryPage(page) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE2600))
                        ) {
                            Text("Повторить", color = Color.White)
                        }
                    }
                }
            } else if (pageVideos != null && pageVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(headerScrollModifier)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isModel) {
                            "У этой модели пока нет опубликованных видео"
                        } else {
                            "У этого канала пока нет опубликованных видео"
                        },
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (pageVideos != null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = pageVideos,
                        key = { it.id }
                    ) { video ->
                        ChannelVideoItem(
                            item = video,
                            isFavorite = isFavorite(video.id),
                            isDownloaded = isDownloaded(video.id),
                            onOpenVideo = onOpenVideo,
                            onFavoriteAdd = onFavoriteAdd,
                            onFavoriteRemove = onFavoriteRemove,
                            onDownload = onDownload,
                            onSaveToGallery = onSaveToGallery,
                        )
                    }
                }
            }
            if (isPageLoading) {
                TopLoadingBar(modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Preview
@Composable
private fun ChannelVideosPagerPreview() {
    val sampleVideos = List(4) { index ->
        ItemsX(
            id = index.toLong(),
            title = "Video $index",
            duration = "12:34",
            views = "50K",
            channel = "Channel",
            href = "/video$index",
            nameProfile = "Channel",
            linkProfile = "/channels/channel"
        )
    }
    val gridState = rememberLazyGridState()
    val pagerState = rememberPagerState { 1 }

    XvideosTheme(darkTheme = true) {
        ChannelVideosPager(
            pagerState = pagerState,
            pagesCache = mapOf(0 to sampleVideos),
            loadingPages = emptySet(),
            errorPages = emptyMap(),
            currentSort = ChannelSortOrder.NEW,
            selectedModel = null,
            isModel = false,
            getGridState = { gridState },
            onLoadPage = {},
            onRetryPage = {},
            onOpenVideo = {},
            isFavorite = { false },
            isDownloaded = { false },
            onFavoriteAdd = {},
            onFavoriteRemove = {},
            onDownload = {},
            onSaveToGallery = {},
        )
    }
}
