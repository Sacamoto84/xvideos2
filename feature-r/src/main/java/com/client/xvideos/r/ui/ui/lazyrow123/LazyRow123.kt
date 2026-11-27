package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.common.ui.atom.FloatingScrollButtons
import com.client.xvideos.common.ui.scroll.LazyGridFastScroll
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.toRUserMessage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyRow123(
    host: LazyRow123Host,
    modifier: Modifier = Modifier,
    onClickOpenProfile: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentBeforeList: (@Composable () -> Unit)? = null,
    isRunLike: Boolean = false,
    showScrollButtons: Boolean = true,
    onAppendLoaded: (LazyPagingItems<GifsInfo>) -> Unit = {},
    emptyMessage: String? = null,
    enableFastScroll: Boolean = true,
    fastScrollSessionKey: Any? = null,
) {
    val listGifs = host.pager.collectAsLazyPagingItems()

    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }
    val gridState = host.position.rememberState()
    val haptic = LocalHapticFeedback.current
    val refreshState = listGifs.loadState.refresh
    // Локальный источник может завершить refresh до следующего кадра. Отдельный
    // флаг жеста тогда остался бы включённым.
    val isRefreshing = refreshState is LoadState.Loading && listGifs.itemCount > 0
    LaunchedEffect(refreshState) {
        // Старые ролики остаются на экране, строка ошибки — в конце списка:
        // без сообщения погасший индикатор читался бы как «готово».
        refreshFailureToAnnounce(refreshState, listGifs.itemCount, host.announcedRefreshFailure)?.let { failure ->
            host.announcedRefreshFailure = failure
            SnackBar.error(failure.toRUserMessage())
        }
    }
    val pullToRefreshState = rememberPullToRefreshState()

    // Выбор пользователя плюс запас широкого окна; сам выбор в host не меняется.
    val gridColumns = adaptiveUserGridColumns(host.columns)

    // Ручка читает позицию отдельно, чтобы при прокрутке не перекомпоновывать весь экран.
    val firstMediaIndex = if (contentBeforeList != null) 1 else 0
    val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(
        gridState = gridState,
        itemsToIgnore = firstMediaIndex,
        numberOfColumns = gridColumns
    )

    /** Показывать ли кнопку "вверх" */
    val showScrollToTop by remember(gridState) {
        derivedStateOf { gridState.firstVisibleItemIndex > 2 }
    }

    /** Показывать ли кнопку "вниз" */
    val showScrollToBottom by rememberCanScrollToBottom(gridState)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            listGifs.refresh()
        },
        state = pullToRefreshState,
        indicator = {
            Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding()),
                isRefreshing = isRefreshing,
                containerColor = Color.White,
                color = Color.Black,
                state = pullToRefreshState
            )
        },
    ) {

        LazyRow123Content(
            gridState = gridState,
            host = host,
            listGifs = listGifs,
            columns = gridColumns,
            modifier = modifier.hazeSource(hazeState),
            onClickOpenProfile = onClickOpenProfile,
            contentPadding = contentPadding,
            contentBeforeList = contentBeforeList,
            isRunLike = isRunLike,
            onAppendLoaded = onAppendLoaded
        )

        if (shouldShowEmptyMessage(emptyMessage, listGifs.itemCount, refreshState, listGifs.loadState.append)) {
            Text(
                emptyMessage.orEmpty(),
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }

        if (enableFastScroll) {
            LazyGridFastScroll(
                hazeState = hazeState,
                state = gridState,
                modifier = Modifier.matchParentSize(),
                sessionKey = fastScrollSessionKey ?: gridState,
                visibleRange = { scrollPercent.value },
                firstItemIndex = firstMediaIndex,
                itemCount = { listGifs.itemSnapshotList.items.size },
                bottomClearance = if (showScrollButtons) {
                    Theme.ScrollFab.size * 2 + Theme.ScrollFab.spacing + 16.dp
                } else {
                    0.dp
                },
            )
        }

        //---- Floating Buttons "Вверх" и "Вниз" ----
        FloatingScrollButtons(
            visible = showScrollButtons,
            showScrollToTop = showScrollToTop,
            showScrollToBottom = showScrollToBottom,
            hazeState = hazeState,
            contentColor = Theme.ScrollFab.contentColorR,
            onScrollToTop = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch { gridState.scrollToItem(0) }
            },
            onScrollToBottom = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch {
                    val totalItems = gridState.layoutInfo.totalItemsCount
                    if (totalItems > 0) {
                        gridState.scrollToItem(totalItems - 1)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

private fun shouldShowEmptyMessage(
    emptyMessage: String?,
    itemCount: Int,
    refreshState: LoadState,
    appendState: LoadState,
): Boolean {
    if (emptyMessage == null || itemCount != 0) return false
    if (refreshState !is LoadState.NotLoading || appendState !is LoadState.NotLoading) return false
    return appendState.endOfPaginationReached
}
