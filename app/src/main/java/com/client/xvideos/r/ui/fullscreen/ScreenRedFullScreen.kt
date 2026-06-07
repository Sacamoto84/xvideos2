package com.client.xvideos.r.ui.fullscreen

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.transitions.ScreenTransition
import com.client.xvideos.R
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.expand_menu_video.ExpandMenuVideo
import com.client.xvideos.r.common.expand_menu_video.ExpandMenuVideoTags
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.video.CanvasTimeDurationLine1
import com.client.xvideos.r.common.video.RedVideoPlayerWithMenu
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.explorer.ScreenRedExplorer
import com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedControls_Container_Line0
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.RFeedSessionStore
import com.redgifs.common.downloader.ui.DownloadIndicator
import com.redgifs.common.video.PlayerControls
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject
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
            val downloadList by vm.downloadRed.downloadList.collectAsState()
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

@Composable
private fun RedFullScreenOverlay(
    item: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator,
    downloadList: List<GifsInfo>,
    haptic: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable(onClick = { navigator.push(ScreenRedProfile(item.userName)) }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val user = UsersRed.listAllUsers.firstOrNull { it.username == item.userName }
            if (user?.profileImageUrl != null) {
                UrlImage(
                    user.profileImageUrl,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .size(40.dp)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            if (vm.savedRed.collections.collectionList.any { collection -> collection.items.any { it.id == item.id } }) {
                Icon(
                    painter = painterResource(R.drawable.collection_multi_input_svgrepo_com),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(18.dp)
                )
            }

            if (vm.savedRed.creators.list.any { it.username == item.userName }) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(22.dp)
                )
            }

            if (vm.savedRed.likes.list.any { it.id == item.id }) {
                Icon(
                    Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(22.dp)
                )
            }

            if (downloadList.any { it.id == item.id }) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp, end = 6.dp).size(20.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    vm.autoRotate = !vm.autoRotate
                    haptic()
                }
            ) {
                Icon(
                    Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            ExpandMenuVideoTags(
                item = item,
                modifier = Modifier,
                onClick = { tag ->
                    vm.search.searchText.value = TextFieldValue(text = tag, selection = TextRange(tag.length))
                    vm.search.searchTextDone.value = tag
                    ScreenRedExplorer.screenType = 0
                    navigator.pop()
                },
            )

            ExpandMenuVideo(
                item = item,
                modifier = Modifier,
                onClick = {},
                haptic = haptic,
                onRunLike = {},
                onRefresh = {},
                isCollection = false,
                block = { vm.block },
                redApi = { vm.redApi },
                savedRed = { vm.savedRed },
                downloadRed = { vm.downloadRed }
            )
        }
    }
}

class ScreenRedFullScreenSM @Inject constructor(
    val connectivityObserver: ConnectivityObserver,
    val downloadRed: DownloadRed,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val search: R_SearchExplorer,
) : ScreenModel {

    var play by mutableStateOf(true)
    var mute by mutableStateOf(true)
    var autoRotate by mutableStateOf(false)

    var enableAB by mutableStateOf(false)
    var timeA by mutableFloatStateOf(3f)
    var timeB by mutableFloatStateOf(6f)

    var currentPlayerControls by mutableStateOf<PlayerControls?>(null)

    var currentPlayerTime by mutableFloatStateOf(0f)
    var currentPlayerDuration by mutableIntStateOf(0)

    var bufferIng by mutableStateOf(false)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedFullScreen {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedFullScreenSM::class)
    abstract fun bindScreenRedFullScreenModel(screenModel: ScreenRedFullScreenSM): ScreenModel
}
