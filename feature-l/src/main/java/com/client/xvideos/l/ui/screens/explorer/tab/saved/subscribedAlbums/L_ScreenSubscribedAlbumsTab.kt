package com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.molecule.SubscribedAlbumUnlikeDialog
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.molecule.SubscribedAlbumsEmptyOrErrorState
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.molecule.SubscribedAlbumsGrid
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.model.SubscribedAlbumsUiState
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Экран подписанных альбомов пользователя с сервера L.
 * Расположен в графе Savable (L_SavedTab).
 * Автоматически использует user_id текущей авторизованной сессии.
 */
object L_ScreenSubscribedAlbumsTab : Screen {

    override val key: ScreenKey = "L_ScreenSubscribedAlbumsTab"

    private fun readResolve(): Any = L_ScreenSubscribedAlbumsTab

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLSubscribedAlbumsSM = getScreenModel()
        val haptic = LocalHapticFeedback.current

        val albums by vm.albums.collectAsStateWithLifecycle()
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()
        val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
        val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()

        val state = vm.state
        val topInset = getTopInsetDp()
        val pullToRefreshState = rememberPullToRefreshState()

        val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(state, itemsToIgnore = 1)

        // Пагинация: автоматическая подгрузка следующей страницы при приближении к концу списка
        val shouldLoadMore by remember(state) {
            derivedStateOf {
                val totalItems = state.layoutInfo.totalItemsCount
                val lastVisibleItem = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 1 && lastVisibleItem >= totalItems - 4
            }
        }

        // После сбоя подгрузка сама не повторяется: её пробуют снова, когда
        // пользователь ушёл от конца списка и вернулся.
        val nextPageFailed by vm.nextPageFailed.collectAsStateWithLifecycle()
        val canLoadMore = vm.hasMore && !isLoading && errorMessage == null && !nextPageFailed
        LaunchedEffect(shouldLoadMore, canLoadMore) {
            if (shouldLoadMore && canLoadMore) {
                vm.loadNextPage()
            }
        }
        LaunchedEffect(shouldLoadMore) {
            if (!shouldLoadMore) vm.onListEndLeft()
        }

        var itemPendingServerUnlike by remember { mutableStateOf<AlbumDetails?>(null) }
        val onDismissUnlike = remember { { itemPendingServerUnlike = null } }

        BackHandler(enabled = itemPendingServerUnlike != null, onBack = onDismissUnlike)

        itemPendingServerUnlike?.let { pending ->
            val onConfirmUnlike = remember(pending, vm, haptic) {
                {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    vm.unlikeAlbum(pending)
                    itemPendingServerUnlike = null
                }
            }
            SubscribedAlbumUnlikeDialog(
                album = pending,
                onConfirm = onConfirmUnlike,
                onDismiss = onDismissUnlike
            )
        }

        val onAlbumClick = remember(navigator) {
            { albumId: Long? ->
                if (albumId != null) {
                    navigator.push(ScreenLAlbum(albumId))
                } else {
                    SnackBar.error("Не удалось открыть альбом: пустой id")
                }
            }
        }
        val onAlbumLongClick = remember(haptic) {
            { item: AlbumDetails ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                itemPendingServerUnlike = item
            }
        }
        val onRetry = remember(vm) { { vm.loadInitial() } }
        val handleRefresh = remember(haptic, vm) {
            {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                vm.refresh()
            }
        }
        val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }

        val uiState = remember(albums, isLoading, isRefreshing, errorMessage) {
            SubscribedAlbumsUiState(
                albums = albums,
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                errorMessage = errorMessage
            )
        }

        SubscribedAlbumsTabContent(
            state = state,
            uiState = uiState,
            topInset = topInset,
            pullToRefreshState = pullToRefreshState,
            scrollPercentProvider = scrollPercentProvider,
            onAlbumClick = onAlbumClick,
            onAlbumLongClick = onAlbumLongClick,
            onRetry = onRetry,
            onRefresh = handleRefresh
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscribedAlbumsTabContent(
    state: LazyGridState,
    uiState: SubscribedAlbumsUiState,
    topInset: Dp,
    pullToRefreshState: PullToRefreshState,
    scrollPercentProvider: () -> Pair<Float, Float>,
    onAlbumClick: (Long?) -> Unit,
    onAlbumLongClick: (AlbumDetails) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background),
        state = pullToRefreshState,
        indicator = {
            Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset),
                isRefreshing = uiState.isRefreshing,
                containerColor = Theme.tabLevel1,
                color = Theme.L.red,
                state = pullToRefreshState
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.albums.isEmpty() && !uiState.isLoading && !uiState.isRefreshing) {
                SubscribedAlbumsEmptyOrErrorState(
                    topInset = topInset,
                    errorMessage = uiState.errorMessage,
                    onRetry = onRetry,
                    onRefresh = onRefresh
                )
            } else {
                SubscribedAlbumsGrid(
                    state = state,
                    albums = uiState.albums,
                    topInset = topInset,
                    isLoading = uiState.isLoading,
                    onAlbumClick = onAlbumClick,
                    onAlbumLongClick = onAlbumLongClick
                )

                // Скроллбар
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .width(2.dp)
                ) {
                    VerticalScrollbar(scrollPercentProvider)
                }
            }

            if (uiState.isLoading && uiState.albums.isEmpty() && !uiState.isRefreshing) {
                CircularProgressIndicator(
                    color = Theme.L.red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SubscribedAlbumsTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        SubscribedAlbumsTabContent(
            state = rememberLazyGridState(),
            uiState = SubscribedAlbumsUiState(),
            topInset = 24.dp,
            pullToRefreshState = rememberPullToRefreshState(),
            scrollPercentProvider = { 0f to 1f },
            onAlbumClick = {},
            onAlbumLongClick = {},
            onRetry = {},
            onRefresh = {}
        )
    }
}
