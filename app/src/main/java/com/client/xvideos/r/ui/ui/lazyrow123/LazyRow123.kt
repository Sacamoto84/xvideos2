package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.navigator.LocalNavigator
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.explorer.ScreenRedExplorer
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreen
import com.client.xvideos.r.ui.profile.atom.VerticalScrollbar
import com.client.xvideos.r.ui.profile.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyRow123(
    host: LazyRow123Host,
    modifier: Modifier = Modifier,
    onClickOpenProfile: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentBeforeList: @Composable (() -> Unit) = {},
    isRunLike: Boolean = false,
    onAppendLoaded: (LazyPagingItems<GifsInfo>) -> Unit = {},
) {

    val listGifs = host.pager.collectAsLazyPagingItems()

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    val scrollPercent by rememberVisibleRangePercentIgnoringFirstNForGrid(
        gridState = host.state,
        itemsToIgnore = 0,
        numberOfColumns = host.columns
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                isRefreshing = true
                delay(500)
                isRefreshing = false
            }
            listGifs.refresh()
        },
        state = pullToRefreshState,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = Color.White,
                color = Color.Black,
                state = pullToRefreshState
            )
        },
    ) {

        LazyRow123Content(
            host = host,
            listGifs = listGifs,
            modifier = modifier,
            onClickOpenProfile = onClickOpenProfile,
            contentPadding = contentPadding,
            contentBeforeList = contentBeforeList,
            isRunLike = isRunLike,
            onAppendLoaded = onAppendLoaded
        )

        //---- Скролл ----
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .width(2.dp)
        ) { VerticalScrollbar(scrollPercent) }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyRow123Content(
    host: LazyRow123Host,
    listGifs: LazyPagingItems<GifsInfo>,
    modifier: Modifier = Modifier,
    onClickOpenProfile: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentBeforeList: @Composable (() -> Unit) = {},
    isRunLike: Boolean = false,
    onAppendLoaded: (LazyPagingItems<GifsInfo>) -> Unit = {},
) {
    SideEffect { Timber.d("!!! LazyRow123::SideEffect columns: ${host.columns}") }

    val isConnected by host.isConnected.collectAsStateWithLifecycle()
    val state = host.state

    val navigator = LocalNavigator.current
    val downloadList by host.downloadRed.downloadList.collectAsState()
    val loadState = listGifs.loadState
    var wasAppendLoading by remember { mutableStateOf(false) }

    LaunchedEffect(loadState.refresh) {
        if (loadState.refresh is LoadState.NotLoading) {
            onAppendLoaded(listGifs)
        }
    }

    LaunchedEffect(loadState.append) {
        if (loadState.append is LoadState.Loading && !wasAppendLoading) {
            wasAppendLoading = true
            onAppendLoaded(listGifs)
        }
        if (loadState.append !is LoadState.Loading) {
            wasAppendLoading = false
        }
    }

    LaunchedEffect(host.returnToIndex, listGifs.itemCount, host.columns) {
        val targetIndex = host.returnToIndex
        if (targetIndex >= 0 && listGifs.itemCount > targetIndex) {
            state.scrollToItem(targetIndex + 1)
            host.returnToIndex = -1
        }
    }

    LazyRow123ContentStateless(
        columns = host.columns,
        state = state,
        itemCount = listGifs.itemCount,
        loadState = loadState,
        itemKey = listGifs.itemKey { it.id },
        modifier = modifier,
        contentPadding = contentPadding,
        contentBeforeList = contentBeforeList,
    ) { index ->
        listGifs[index]?.let { item ->
            val isDownloaded = remember(item.id, downloadList) {
                downloadList.any { it.id == item.id }
            }

            LazyRow123GridItem(
                item = item,
                index = index,
                host = host,
                isConnected = isConnected,
                isDownloaded = isDownloaded,
                isRunLike = isRunLike,
                onItemClick = {
                    host.currentIndex = index
                    host.returnToIndex = index
                    host.block.blockItem = item
                    navigator?.push(ScreenRedFullScreen(item, host.feedKey, index))
                },
                onRefresh = { listGifs.refresh() },
                onClickOpenProfile = onClickOpenProfile,
                onTagClick = { tag ->
                    host.search.searchText.value = TextFieldValue(tag, TextRange(tag.length))
                    host.search.searchTextDone.value = tag
                    ScreenRedExplorer.screenType = 0
                    navigator?.popAll()
                }
            )
        } ?: Box(
            modifier = Modifier
                .padding(1.dp)
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyRow123ContentStateless(
    columns: Int,
    state: LazyGridState,
    itemCount: Int,
    loadState: CombinedLoadStates,
    itemKey: ((Int) -> Any)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentBeforeList: @Composable (() -> Unit) = {},
    itemContent: @Composable (Int) -> Unit
) {
    val isAnyLoading = loadState.refresh is LoadState.Loading ||
            loadState.append is LoadState.Loading ||
            loadState.prepend is LoadState.Loading

    Box(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item(key = "before", span = { GridItemSpan(maxLineSpan) }) { contentBeforeList() }

            items(
                count = itemCount,
                key = itemKey,
                contentType = { "video_grid_item" }
            ) { index ->
                itemContent(index)
            }

            if (loadState.append is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        if (loadState.refresh is LoadState.Loading && itemCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (isAnyLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(2.dp),
                color = Color.Red
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LazyRow123ContentPreview() {
    XvideosTheme {
        val sampleItems = List(10) { index ->
            GifsInfo(
                id = index.toString(),
                userName = "User $index",
                description = "Sample description $index"
            )
        }

        val notLoading = LoadState.NotLoading(false)
        val mockLoadState = CombinedLoadStates(
            refresh = notLoading,
            prepend = notLoading,
            append = notLoading,
            source = LoadStates(
                refresh = notLoading,
                prepend = notLoading,
                append = notLoading
            )
        )

        LazyRow123ContentStateless(
            columns = 2,
            state = rememberLazyGridState(),
            itemCount = sampleItems.size,
            loadState = mockLoadState,
            itemKey = { index -> sampleItems[index].id },
            contentPadding = PaddingValues(8.dp)
        ) { index ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Item $index", color = Color.White)
            }
        }
    }
}


