package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.lifecycle.SlidingWindowEffect
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.common.videoplayer.feed.rememberFeedPlayerState
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreenSM
import com.client.xvideos.r.ui.fullscreen.peekUrl
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max

@OptIn(UnstableApi::class)
@Suppress("LongMethod")
@Composable
fun RedFullScreenFeed(
    host: LazyRow123Host,
    startIndex: Int,
    fallbackItem: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator
) {
    val listGifs = host.pager.collectAsLazyPagingItems()
    val downloadedKeys by vm.downloadRed.downloadedVideoKeys.collectAsStateWithLifecycle()
    val appendExtra = if (listGifs.loadState.append is LoadState.Loading && listGifs.itemCount > 0) 1 else 0
    val pagerCount = max(startIndex + 1, listGifs.itemCount + appendExtra)
    val pagerState = rememberPagerState(initialPage = startIndex.coerceAtLeast(0)) { pagerCount.coerceAtLeast(1) }
    var isVideoBuffering by remember { mutableStateOf(false) }

    val feedState = rememberFeedPlayerState()

    SlidingWindowEffect(
        itemCountProvider = { listGifs.itemCount },
        currentItemProvider = { pagerState.currentPage },
        maxLookbehind = 2,
        maxLookahead = 4,
        batchSize = 3,
        onRangeEnterWindow = { range ->
            feedState.addRange(range) { i ->
                listGifs.peekUrl(i, downloadedKeys)
            }
        },
        onRangeLeaveWindow = { range ->
            feedState.removeRange(range)
        },
    )

    LaunchedEffect(listGifs, feedState) {
        snapshotFlow { listGifs.itemCount }
            .distinctUntilChanged()
            .collect {
                feedState.retryPending { i -> listGifs.peekUrl(i, downloadedKeys) }
            }
    }

    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableIntStateOf(0) }

    val handleBack: () -> Unit = remember(navigator) {
        {
            if (isCurrentPageZoomed) {
                resetZoomTrigger++
            } else {
                navigator.pop()
            }
        }
    }

    BackHandler(enabled = isCurrentPageZoomed) {
        resetZoomTrigger++
    }
    BackHandler(enabled = !isCurrentPageZoomed, onBack = handleBack)

    LaunchedEffect(pagerState, host) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                isCurrentPageZoomed = false
                feedState.updateCurrentPage(page)
                feedState.retryPending { i -> listGifs.peekUrl(i, downloadedKeys) }
                host.currentIndex = page
                host.returnToIndex = page
                vm.play = true
                vm.currentPlayerTime = 0f
                vm.currentPlayerDuration = 0
                vm.enableAB = false
                vm.resetSpeed()
            }
    }

    RedFullScreenFeedScaffold(vm = vm, isVideoBuffering = isVideoBuffering) { bottomPadding ->
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isCurrentPageZoomed,
            beyondViewportPageCount = 1
        ) { index ->
            val currentItem = if (index < listGifs.itemCount) listGifs[index] else null
            val isCurrentPage = pagerState.currentPage == index

            if (currentItem != null) {
                RedFullScreenPage(
                    item = currentItem,
                    vm = vm,
                    navigator = navigator,
                    feedState = feedState,
                    downloadedKeys = downloadedKeys,
                    index = index,
                    bottomPadding = bottomPadding,
                    play = vm.play && isCurrentPage,
                    isCurrentPage = isCurrentPage,
                    showOverlay = isCurrentPage,
                    onBuffering = { buffering ->
                        if (isCurrentPage) {
                            isVideoBuffering = buffering
                        }
                    },
                    onZoomChanged = { zoomed ->
                        if (isCurrentPage) {
                            isCurrentPageZoomed = zoomed
                        }
                    },
                    resetZoomTrigger = if (isCurrentPage) resetZoomTrigger else 0,
                    onBack = handleBack
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (index == startIndex) {
                        RedFullScreenPage(
                            item = fallbackItem,
                            vm = vm,
                            navigator = navigator,
                            feedState = feedState,
                            downloadedKeys = downloadedKeys,
                            index = index,
                            bottomPadding = bottomPadding,
                            play = vm.play,
                            isCurrentPage = true,
                            showOverlay = true,
                            onBuffering = { isVideoBuffering = it },
                            onZoomChanged = { isCurrentPageZoomed = it },
                            resetZoomTrigger = resetZoomTrigger,
                            onBack = handleBack
                        )
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun RedFullScreenFeedPreview() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("RedFullScreenFeed", color = Color.White)
    }
}
