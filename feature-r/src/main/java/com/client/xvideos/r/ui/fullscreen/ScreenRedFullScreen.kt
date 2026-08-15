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
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.lifecycle.SlidingWindowEffect
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
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
import com.client.xvideos.common.videoplayer.feed.FeedPlayerState
import com.client.xvideos.common.videoplayer.feed.rememberFeedPlayerState
import com.client.xvideos.r.ui.video.CanvasTimeDurationLine1
import com.client.xvideos.r.ui.video.RedPooledVideoPlayer
import com.client.xvideos.r.common.downloader.downloadedVideoKey
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedControls_Container_Line0
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.RFeedSessionStore
import com.client.xvideos.common.ui.atom.DownloadIndicator
import kotlinx.coroutines.flow.distinctUntilChanged
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun RedFullScreenFeed(
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
        // Именно itemCount пейджинга, а не pagerState.pageCount: счётчик страниц
        // пейджера больше списка на «хвост» дозагрузки и на startIndex до прихода
        // данных. Для таких индексов url не существует, они оседали в ожидании
        // догрева навсегда.
        itemCountProvider = { listGifs.itemCount },
        // Тот же currentPage, что уходит в updateCurrentPage ниже. Раньше окно
        // велось по settledPage, а ранжирование — по currentPage, и ранжирование
        // могло ссылаться на индекс, ещё не вошедший в окно.
        currentItemProvider = { pagerState.currentPage },
        maxLookbehind = 2,
        maxLookahead = 4,
        // Батч применяется только при восстановлении после прыжка: в обычной
        // прокрутке SlidingWindowEffect каждый раз пересобирает окно целиком
        // (см. ветку else в его reconcile), и порционная догрузка не включается.
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

    // Пейджинг отдаёт элементы асинхронно, и первое окно на старте почти всегда пустое:
    // `peekUrl` для стартовых индексов вернул null, а повторного onRangeEnterWindow не будет.
    // Поэтому догреваем окно ещё и по приходу новых элементов, а не только на смене страницы.
    LaunchedEffect(listGifs, feedState) {
        snapshotFlow { listGifs.itemCount }
            .distinctUntilChanged()
            .collect {
                feedState.retryPending { i -> listGifs.peekUrl(i, downloadedKeys) }
            }
    }

    LaunchedEffect(pagerState, host) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                feedState.updateCurrentPage(page)
                // Догреваем то, чего не было в списке на момент входа окна:
                // на первом кадре экрана пейджинг ещё пуст, и повторного
                // onRangeEnterWindow для стартового окна уже не будет.
                feedState.retryPending { i -> listGifs.peekUrl(i, downloadedKeys) }
                host.currentIndex = page
                host.returnToIndex = page
                vm.play = true
                // currentPlayerControls здесь не обнуляем: их отзывает сама
                // страница в onPlayerControlsRelease, и только свои.
                vm.currentPlayerTime = 0f
                vm.currentPlayerDuration = 0
            }
    }

    RedFullScreenScaffold(vm = vm, isVideoBuffering = isVideoBuffering) { bottomPadding ->
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Плееров теперь ровно столько, сколько в пуле (3): держать в композиции
            // пять страниц незачем — прогрев соседей делает preload-менеджер.
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
                    }
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
    val downloadedKeys by vm.downloadRed.downloadedVideoKeys.collectAsStateWithLifecycle()

    val feedState = rememberFeedPlayerState(poolCapacity = 1)

    // Без этого statusControl.currentPlayingIndex остаётся C.INDEX_UNSET, политика
    // всегда отдаёт CACHED_ONLY, и preload-менеджер на этом экране не делает ничего.
    LaunchedEffect(feedState) { feedState.updateCurrentPage(0) }

    RedFullScreenScaffold(vm = vm, isVideoBuffering = isVideoBuffering) { bottomPadding ->
        RedFullScreenPage(
            item = item,
            vm = vm,
            navigator = navigator,
            feedState = feedState,
            downloadedKeys = downloadedKeys,
            index = 0,
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
    feedState: FeedPlayerState,
    downloadedKeys: Set<String>,
    index: Int,
    bottomPadding: Dp,
    play: Boolean,
    isCurrentPage: Boolean,
    showOverlay: Boolean,
    onBuffering: (Boolean) -> Unit
) {
    val videoUri = remember(item.id, item.userName, downloadedKeys) { redVideoUrl(item, downloadedKeys) }

    Box(Modifier.fillMaxSize()) {
        RedPooledVideoPlayer(
            feedState = feedState,
            index = index,
            url = videoUri,
            modifier = Modifier.padding(bottom = bottomPadding),
            play = play,
            isMute = vm.mute,
            isCurrentPage = isCurrentPage,
            autoRotate = vm.autoRotate,
            timeA = vm.timeA,
            timeB = vm.timeB,
            enableAB = vm.enableAB,
            onTimeChanged = { position, duration ->
                if (isCurrentPage) {
                    vm.currentPlayerTime = position
                    vm.currentPlayerDuration = duration
                }
            },
            onPlayerControlsReady = { controls ->
                if (isCurrentPage) {
                    vm.currentPlayerControls = controls
                }
            },
            onPlayerControlsRelease = { controls ->
                // Сравнение по ссылке: страница отзывает только свои controls и не
                // затирает те, что успела выставить пришедшая ей на смену.
                if (vm.currentPlayerControls === controls) vm.currentPlayerControls = null
            },
            onClick = { if (isCurrentPage) vm.play = !vm.play },
            isBuferring = { buffering ->
                if (isCurrentPage) {
                    onBuffering(buffering)
                }
            },
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

/**
 * Url элемента ленты по индексу для окна предзагрузки. `peek` (а не `get`) —
 * намеренно: он не дёргает пейджинг на подгрузку соседних страниц. Границу всё
 * равно проверяем: `peek` за пределами `itemCount` бросает IndexOutOfBoundsException.
 */
private fun LazyPagingItems<GifsInfo>.peekUrl(index: Int, downloadedKeys: Set<String>): String? {
    if (index < 0 || index >= itemCount) return null
    return peek(index)?.let { redVideoUrl(it, downloadedKeys) }
}

/**
 * Адрес видео для элемента ленты: локальный файл, если ролик уже скачан,
 * иначе HLS с api.redgifs.com. Общая точка для страницы и для предзагрузки —
 * ключи preload-менеджера обязаны совпадать с тем, что реально играет плеер.
 *
 * Скачанность берётся из готового набора ключей, а не из `File.exists()`:
 * функция зовётся из окна предзагрузки на главном потоке для каждого индекса.
 */
private fun redVideoUrl(item: GifsInfo, downloadedKeys: Set<String>): String =
    if (downloadedVideoKey(item.userName, item.id) in downloadedKeys) {
        "${AppPath.r_cache_download}/${item.userName}/${item.id}.mp4"
    } else {
        "https://api.redgifs.com/v2/gifs/${item.id.lowercase()}/hd.m3u8"
    }


