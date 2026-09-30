package com.client.xvideos.l.ui.screens.screenFullScreen

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.noRippleClickable
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuViewModel
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenFullScreen.molecule.FullScreenBottomThumbnails
import com.client.xvideos.l.ui.screens.screenFullScreen.molecule.FullScreenTopControls
import com.client.xvideos.l.ui.screens.screenFullScreen.model.FullScreenUiState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
class L_FullScreenImage(
    val item: PicsDetails,
    val albumName: String,
    val idAlbum: String = "",
    val payloadKey: String = "",
    val autoPlay: Boolean = false,
    val isAnimated: Boolean = false,
    val expandMenu: ExpandMenuType,
    val isCollection: Boolean = false,
    @IgnoredOnParcel val onClose: (Int) -> Unit = {},
) : Screen, Parcelable {

    @IgnoredOnParcel
    override val key: ScreenKey = "L_FullScreenImage:$albumName:${item.id}:$payloadKey"

    @OptIn(
        ExperimentalFoundationApi::class,
        DelicateCoroutinesApi::class
    )
    @Composable
    override fun Content() {

        val filteredPic = remember(payloadKey, item) {
            LFullScreenPayload.get(payloadKey).ifEmpty { listOf(item) }
        }

        val expandMenuViewModel: ExpandMenuViewModel = hiltViewModel()
        val navigator = LocalNavigator.currentOrThrow

        var isClosing by remember { mutableStateOf(false) }
        var isCurrentPageZoomed by remember { mutableStateOf(false) }
        var resetZoomTrigger by remember { mutableIntStateOf(0) }
        var corruptCancel by remember { mutableStateOf(false) }
        var showInfoDialog by remember { mutableStateOf(false) }
        var isFullScreen by remember { mutableStateOf(false) }

        val verticalPager by Settings.l_fullscreen_vertical_pager.field.collectAsStateWithLifecycle()
        val videoMuted by Settings.l_fullscreen_video_muted.field.collectAsStateWithLifecycle()

        val initialIndex = remember(filteredPic, item) { resolveInitialIndex(filteredPic, item) }
        val pagerState = rememberPagerState(initialIndex, pageCount = { filteredPic.size })
        val lazyRowState = rememberLazyListState(cacheWindow = LazyLayoutCacheWindow(ahead = 200.dp, behind = 200.dp))

        LaunchedEffect(isClosing) {
            if (isClosing) {
                runCatching {
                    onClose(if (corruptCancel) pagerState.currentPage else -1)
                }
                navigator.pop()
            }
        }

        val onDismissInfo: () -> Unit = remember { { showInfoDialog = false } }
        val onShowInfo: () -> Unit = remember { { showInfoDialog = true } }
        val onResetZoom: () -> Unit = remember { { resetZoomTrigger++ } }
        val onExitFullScreen: () -> Unit = remember { { isFullScreen = false } }
        val onCloseScreen: () -> Unit = remember { { isClosing = true } }

        BackHandler(enabled = showInfoDialog, onBack = onDismissInfo)
        BackHandler(enabled = !showInfoDialog && isCurrentPageZoomed, onBack = onResetZoom)
        BackHandler(enabled = !showInfoDialog && !isCurrentPageZoomed && isFullScreen, onBack = onExitFullScreen)
        BackHandler(enabled = !showInfoDialog && !isCurrentPageZoomed && !isFullScreen, onBack = onCloseScreen)

        val currentIndex = pagerState.currentPage

        LaunchedEffect(currentIndex) {
            isCurrentPageZoomed = false
            if (currentIndex != initialIndex) { corruptCancel = true }
        }

        LaunchedEffect(currentIndex) {
            if (filteredPic.isNotEmpty()) {
                lazyRowState.scrollToItem(resolveScrollIndex(currentIndex, filteredPic.lastIndex))
            }
        }

        val onAlbumClick: (Long) -> Unit = remember(navigator) {
            { albumId ->
                showInfoDialog = false
                val inStack = navigator.items.any { it is ScreenLAlbum && it.idAlbum == albumId }
                if (inStack) {
                    navigator.popUntil { it is ScreenLAlbum && it.idAlbum == albumId }
                } else {
                    navigator.push(ScreenLAlbum(albumId))
                }
            }
        }

        val uiState = FullScreenUiState(
            item = item,
            filteredPic = filteredPic,
            albumName = albumName,
            idAlbum = idAlbum,
            expandMenu = expandMenu,
            isCollection = isCollection,
            autoPlay = autoPlay,
            currentIndex = currentIndex,
            isFullScreen = isFullScreen,
            showInfoDialog = showInfoDialog,
            verticalPager = verticalPager,
            videoMuted = videoMuted,
            resetZoomTrigger = resetZoomTrigger,
        )

        FullScreenImageContent(
            state = uiState,
            pagerState = pagerState,
            lazyRowState = lazyRowState,
            expandMenuViewModel = expandMenuViewModel,
            onDismissInfo = onDismissInfo,
            onShowInfo = onShowInfo,
            onAlbumClick = onAlbumClick,
            onToggleFullScreen = { isFullScreen = !isFullScreen },
            onZoomChanged = { isCurrentPageZoomed = it },
            onCorruptCancel = { corruptCancel = true }
        )
    }
}

@Composable
fun FullScreenImageContent(
    state: FullScreenUiState,
    pagerState: PagerState,
    lazyRowState: LazyListState,
    expandMenuViewModel: ExpandMenuViewModel,
    onDismissInfo: () -> Unit,
    onShowInfo: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onToggleFullScreen: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    onCorruptCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var rotate by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .checkerboardBackground(squareSize = 12.dp, lightColor = Color(0xFF252525), darkColor = Color(0xFF181818))
            .noRippleClickable(onClick = onToggleFullScreen)
    ) {
        if (state.showInfoDialog) {
            LPictureInfoDialog(
                item = state.filteredPic.getOrNull(state.currentIndex) ?: state.item,
                position = state.currentIndex,
                total = state.filteredPic.size,
                onDismiss = onDismissInfo,
                onAlbumClick = onAlbumClick
            )
        }

        if (state.verticalPager) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1,
                key = { page -> "${state.filteredPic.getOrNull(page)?.url_to_original}#$page" }
            ) { page ->
                LFullScreenPage(
                    pageItem = state.filteredPic.getOrNull(page) ?: state.item,
                    page = page,
                    currentIndex = state.currentIndex,
                    pagerState = pagerState,
                    rotate = rotate,
                    albumName = state.albumName,
                    autoPlay = state.autoPlay,
                    videoMuted = state.videoMuted,
                    seekDragEnabled = true,
                    resetZoomTrigger = state.resetZoomTrigger,
                    onZoomChanged = onZoomChanged,
                    onToggleFullScreen = onToggleFullScreen
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1,
                reverseLayout = false,
                key = { page -> "${state.filteredPic.getOrNull(page)?.url_to_original}#$page" }
            ) { page ->
                LFullScreenPage(
                    pageItem = state.filteredPic.getOrNull(page) ?: state.item,
                    page = page,
                    currentIndex = state.currentIndex,
                    pagerState = pagerState,
                    rotate = rotate,
                    albumName = state.albumName,
                    autoPlay = state.autoPlay,
                    videoMuted = state.videoMuted,
                    seekDragEnabled = false,
                    resetZoomTrigger = state.resetZoomTrigger,
                    onZoomChanged = onZoomChanged,
                    onToggleFullScreen = onToggleFullScreen
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = state.currentIndex.toString(),
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp),
                fontFamily = Theme.L.fontFamilyKarla
            )
        }

        val onRotateToggle = remember { { rotate = !rotate } }
        val onVerticalPagerToggle = remember(state.verticalPager) {
            { Settings.l_fullscreen_vertical_pager.setValue(!state.verticalPager) }
        }
        val onVideoMutedToggle = remember(state.videoMuted) {
            { Settings.l_fullscreen_video_muted.setValue(!state.videoMuted) }
        }

        FullScreenTopControls(
            visible = !state.isFullScreen,
            verticalPager = state.verticalPager,
            videoMuted = state.videoMuted,
            onRotateToggle = onRotateToggle,
            onVerticalPagerToggle = onVerticalPagerToggle,
            onVideoMutedToggle = onVideoMutedToggle,
            onShowInfoDialog = onShowInfo,
            modifier = Modifier.align(Alignment.TopStart),
            expandMenuContent = {
                expandMenuViewModel.ExpandMenu(
                    state.expandMenu,
                    state.filteredPic.getOrNull(pagerState.currentPage) ?: state.item,
                    state.idAlbum,
                    state.isCollection
                )
            }
        )

        expandMenuViewModel.P2pShareHost()

        val onThumbnailClick = remember(coroutineScope, pagerState, onCorruptCancel) {
            { index: Int ->
                coroutineScope.launch { pagerState.scrollToPage(index) }
                onCorruptCancel()
            }
        }

        FullScreenBottomThumbnails(
            visible = !state.isFullScreen,
            lazyRowState = lazyRowState,
            filteredPic = state.filteredPic,
            currentIndex = state.currentIndex,
            albumName = state.albumName,
            onThumbnailClick = onThumbnailClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview
@Composable
private fun L_FullScreenImagePreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .checkerboardBackground(squareSize = 12.dp, lightColor = Color(0xFF252525), darkColor = Color(0xFF181818))
    )
}
