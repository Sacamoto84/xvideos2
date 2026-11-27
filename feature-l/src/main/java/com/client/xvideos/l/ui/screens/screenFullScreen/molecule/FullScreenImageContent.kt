package com.client.xvideos.l.ui.screens.screenFullScreen.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.noRippleClickable
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenu
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuP2pHost
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuViewModel
import com.client.xvideos.l.ui.screens.screenFullScreen.atom.FullScreenPositionLabel
import com.client.xvideos.l.ui.screens.screenFullScreen.model.FullScreenUiState
import com.client.xvideos.l.ui.screens.screenFullScreen.util.checkerboardBackground
import kotlinx.coroutines.launch

@Composable
internal fun FullScreenImageContent(
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

        FullScreenPositionLabel(
            position = state.currentIndex,
            total = state.filteredPic.size,
            modifier = Modifier.align(Alignment.TopStart)
        )

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
                ExpandMenu(
                    state.expandMenu,
                    state.filteredPic.getOrNull(pagerState.currentPage) ?: state.item,
                    state.idAlbum,
                    expandMenuViewModel,
                    isCollection = state.isCollection
                )
            }
        )

        ExpandMenuP2pHost(expandMenuViewModel)

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
private fun FullScreenImageContentPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .checkerboardBackground(squareSize = 12.dp, lightColor = Color(0xFF252525), darkColor = Color(0xFF181818))
    )
}
