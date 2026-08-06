package com.client.xvideos.r.ui.fullscreen

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.transitions.ScreenTransition
import com.client.xvideos.core.R
import com.client.xvideos.common.AppPath
import com.client.xvideos.r.ui.video.CanvasTimeDurationLine1
import com.client.xvideos.r.ui.video.RedVideoPlayerWithMenu
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedControls_Container_Line0
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.RFeedSessionStore
import com.client.xvideos.common.ui.atom.DownloadIndicator
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import kotlin.math.max

@OptIn(ExperimentalVoyagerApi::class)
class ScreenRedFullScreen(
    val item: GifsInfo,
    private val feedKey: String? = null,
    private val startIndex: Int = 0
) : Screen, ScreenTransition {

    override val key: ScreenKey = uniqueScreenKey

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenRedFullScreenSM>()
        val host = remember(feedKey) { feedKey?.let { RFeedSessionStore.get(it) } }

        if (host != null) {
            RedFullScreenFeed(
                host = host,
                startIndex = startIndex,
                fallbackItem = item,
                vm = vm,
                navigator = navigator
            )
        } else {
            RedFullScreenSingle(
                item = item,
                vm = vm,
                navigator = navigator
            )
        }
    }

    override fun enter(lastEvent: StackEvent): EnterTransition {
        return fadeIn(tween(300))
    }

    override fun exit(lastEvent: StackEvent): ExitTransition {
        return fadeOut(tween(300))
    }
}

@Composable
private fun RedFullScreenFeed(
    host: LazyRow123Host,
    startIndex: Int,
    fallbackItem: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator
) {
    val listGifs = host.pager.collectAsLazyPagingItems()
    val appendExtra = if (listGifs.loadState.append is LoadState.Loading && listGifs.itemCount > 0) 1 else 0
    val pagerCount = max(startIndex + 1, listGifs.itemCount + appendExtra)
    val pagerState = rememberPagerState(initialPage = startIndex.coerceAtLeast(0)) { pagerCount.coerceAtLeast(1) }
    var isVideoBuffering by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState, host) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                host.currentIndex = page
                host.returnToIndex = page
                vm.play = true
                vm.currentPlayerControls = null
                vm.currentPlayerTime = 0f
                vm.currentPlayerDuration = 0
            }
    }

    RedFullScreenScaffold(vm = vm, isVideoBuffering = isVideoBuffering) { bottomPadding ->
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 2
        ) { index ->
            val currentItem = if (index < listGifs.itemCount) listGifs[index] else null
            val isCurrentPage = pagerState.currentPage == index

            if (currentItem != null) {
                RedFullScreenPage(
                    item = currentItem,
                    vm = vm,
                    navigator = navigator,
                    bottomPadding = bottomPadding,
                    play = vm.play && isCurrentPage,
                    isCurrentPage = isCurrentPage,
                    showOverlay = isCurrentPage,
                    onBuffering = { buffering ->
                        if (isCurrentPage) {
                            isVideoBuffering = buffering
                        }
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (index == startIndex) {
                        RedFullScreenPage(
                            item = fallbackItem,
                            vm = vm,
                            navigator = navigator,
                            bottomPadding = bottomPadding,
                            play = vm.play,
                            isCurrentPage = true,
                            showOverlay = true,
                            onBuffering = { isVideoBuffering = it }
                        )
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RedFullScreenSingle(
    item: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator
) {
    var isVideoBuffering by remember { mutableStateOf(false) }

    RedFullScreenScaffold(vm = vm, isVideoBuffering = isVideoBuffering) { bottomPadding ->
        RedFullScreenPage(
            item = item,
            vm = vm,
            navigator = navigator,
            bottomPadding = bottomPadding,
            play = vm.play,
            isCurrentPage = true,
            showOverlay = true,
            onBuffering = { isVideoBuffering = it }
        )
    }
}

@Composable
private fun RedFullScreenScaffold(
    vm: ScreenRedFullScreenSM,
    isVideoBuffering: Boolean,
    content: @Composable (bottomPadding: Dp) -> Unit
) {
    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.background(Theme.R.colorCommonBackground)) {
                Box(
                    Modifier
                        .padding(bottom = 1.dp)
                        .clip(RoundedCornerShape(0))
                        .height(32.dp)
                        .fillMaxWidth()
                        .background(Theme.tabLevel0),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CanvasTimeDurationLine1(
                        currentTime = vm.currentPlayerTime,
                        duration = vm.currentPlayerDuration,
                        timeA = vm.timeA,
                        timeB = vm.timeB,
                        timeABEnable = vm.enableAB,
                        play = vm.play,
                        onSeek = { vm.currentPlayerControls?.seekTo(it) },
                        onSeekFinished = {},
                        modifier = Modifier.padding(horizontal = 0.dp),
                        isBuffering = isVideoBuffering
                    )
                }

                Box(modifier = Modifier.background(Theme.tabLevel1)) {
                    FeedControls_Container_Line0(vm)
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        DownloadIndicator(vm.downloadRed.downloader.percent.collectAsStateWithLifecycle().value)
                    }
                }
            }
        }
    ) { padding ->
        content(padding.calculateBottomPadding() / 2)
    }
}

@Composable
private fun RedFullScreenPage(
    item: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator,
    bottomPadding: Dp,
    play: Boolean,
    isCurrentPage: Boolean,
    showOverlay: Boolean,
    onBuffering: (Boolean) -> Unit
) {
    val videoUri = remember(item.id, item.userName) {
        Timber.tag("???").i("Recalculate Red fullscreen video id=${item.id}")
        if (vm.downloadRed.downloader.findVideoInDownload(item.id, item.userName)) {
            "${AppPath.r_cache_download}/${item.userName}/${item.id}.mp4"
        } else {
            "https://api.redgifs.com/v2/gifs/${item.id.lowercase()}/hd.m3u8"
        }
    }

    Box(Modifier.fillMaxSize()) {
        RedVideoPlayerWithMenu(
            modifier = Modifier.padding(bottom = bottomPadding),
            url = videoUri,
            play = play,
            onChangeTime = { time ->
                if (isCurrentPage) {
                    vm.currentPlayerTime = time.first
                    vm.currentPlayerDuration = time.second
                }
            },
            isMute = vm.mute,
            onPlayerControlsReady = { controls ->
                if (isCurrentPage) {
                    vm.currentPlayerControls = controls
                }
            },
            timeA = vm.timeA,
            timeB = vm.timeB,
            enableAB = vm.enableAB,
            onClick = { if (isCurrentPage) vm.play = !vm.play },
            autoRotate = vm.autoRotate,
            isCurrentPage = isCurrentPage,
            isBuferring = { buffering ->
                if (isCurrentPage) {
                    onBuffering(buffering)
                }
            }
        )

        if (showOverlay) {
            val haptic = LocalHapticFeedback.current
            val downloadList by vm.downloadRed.downloadList.collectAsStateWithLifecycle()
            RedFullScreenOverlay(
                item = item,
                vm = vm,
                navigator = navigator,
                downloadList = downloadList,
                haptic = { haptic.performHapticFeedback(HapticFeedbackType.Confirm) }
            )
        }
    }
}


