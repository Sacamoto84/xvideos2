package com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
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
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes.molecule.ServerLikesEmptyOrErrorState
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Экран лайкнутых картинок пользователя с сервера Luscious.
 * Расположен в графе Savable (L_SavedTab).
 */
object L_ScreenServerLikesTab : Screen {

    override val key: ScreenKey = "L_ScreenServerLikesTab"

    private fun readResolve(): Any = L_ScreenServerLikesTab

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val vm: ScreenLServerLikesSM = getScreenModel()
        val haptic = LocalHapticFeedback.current

        val pictures by vm.pictures.collectAsStateWithLifecycle()
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()
        val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
        val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()

        val column by Settings.l_likesTab_column_current_count.field.collectAsStateWithLifecycle()
        LaunchedEffect(column) {
            if (column != 0) {
                vm.host.columns = column
            }
        }

        val topInset = getTopInsetDp()
        val pullToRefreshState = rememberPullToRefreshState()

        // Пагинация: автоматическая подгрузка следующей страницы при прокрутке к концу
        val shouldLoadMore by remember(vm.host.state) {
            derivedStateOf {
                val totalItems = vm.host.state.layoutInfo.totalItemsCount
                val lastVisibleItem = vm.host.state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
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

        val onRetry = remember(vm) { { vm.loadInitial() } }
        val onRefresh = remember(vm) { { vm.refresh() } }
        val handleRefresh = remember(haptic, onRefresh) {
            {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onRefresh()
            }
        }

        ServerLikesTabContent(
            host = vm.host,
            pictures = pictures,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            errorMessage = errorMessage,
            topInset = topInset,
            pullToRefreshState = pullToRefreshState,
            onRefresh = handleRefresh,
            onRetry = onRetry
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerLikesTabContent(
    host: LazyRowPictureDetailsHost,
    pictures: List<PicsDetails>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: String?,
    topInset: Dp,
    pullToRefreshState: PullToRefreshState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemBefore: @Composable () -> Unit = remember(topInset) {
        {
            Box(modifier = Modifier.fillMaxWidth().height(topInset))
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
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
                isRefreshing = isRefreshing,
                containerColor = Theme.tabLevel1,
                color = Theme.L.red,
                state = pullToRefreshState
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (pictures.isEmpty() && !isLoading && !isRefreshing) {
                ServerLikesEmptyOrErrorState(
                    topInset = topInset,
                    errorMessage = errorMessage,
                    onRetry = onRetry,
                    onRefresh = onRefresh
                )
            } else {
                L_LazyRowPictureDetails(
                    host = host,
                    expandMenu = ExpandMenuType.SERVER_LIKES,
                    tag = "l_server_likes",
                    itemBefore = itemBefore
                )
            }

            if (isLoading && pictures.isEmpty() && !isRefreshing) {
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
private fun ServerLikesTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        ServerLikesTabContent(
            host = remember { LazyRowPictureDetailsHost("preview_server_likes") },
            pictures = emptyList(),
            isLoading = false,
            isRefreshing = false,
            errorMessage = null,
            topInset = 24.dp,
            pullToRefreshState = rememberPullToRefreshState(),
            onRefresh = {},
            onRetry = {}
        )
    }
}
