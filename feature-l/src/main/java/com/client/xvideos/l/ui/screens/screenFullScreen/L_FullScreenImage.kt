package com.client.xvideos.l.ui.screens.screenFullScreen

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.ui.statusbar.HideStatusBarEffect
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuViewModel
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenFullScreen.model.FullScreenUiState
import com.client.xvideos.l.ui.screens.screenFullScreen.model.LFullScreenPayload
import com.client.xvideos.l.ui.screens.screenFullScreen.model.resolveInitialIndex
import com.client.xvideos.l.ui.screens.screenFullScreen.model.resolveScrollIndex
import com.client.xvideos.l.ui.screens.screenFullScreen.molecule.FullScreenImageContent
import kotlinx.coroutines.DelicateCoroutinesApi
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
        HideStatusBarEffect()

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
