package com.client.xvideos.xlive.ui.feed.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns
import com.client.xvideos.common.ui.adaptive.adaptiveListPadding
import com.client.xvideos.xlive.model.XLiveRoomItem
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.feed.XLiveFeedActions
import com.client.xvideos.xlive.ui.feed.XLiveFeedState
import com.client.xvideos.xlive.ui.feed.atom.XLiveLoadMoreError
import com.client.xvideos.xlive.ui.feed.atom.XLivePlaceholder
import com.client.xvideos.xlive.ui.room.atom.XLiveMessage
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Догружать, когда до конца сетки осталось столько карточек. */
private const val LOAD_MORE_THRESHOLD = 12

/**
 * Сетка вкладки «Эфиры»: карточки комнат, догрузка в подвале, обновление
 * свайпом вниз. Колонки — адаптивные: две на телефоне, больше на раскрытом
 * складном.
 *
 * Догрузка — здесь же: когда до конца остаётся [LOAD_MORE_THRESHOLD]
 * карточек, сетка зовёт [XLiveFeedActions.onLoadMore].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XLiveRoomsGrid(
    feed: XLiveFeedState,
    gridState: LazyGridState,
    actions: XLiveFeedActions,
    modifier: Modifier = Modifier,
) {
    val currentOnLoadMore by rememberUpdatedState(actions.onLoadMore)
    // Число комнат — в ключе: после догрузки условие проверяется заново, и
    // короткая страница, которая не заполнила экран, тянет следующую.
    LaunchedEffect(gridState, feed.rooms.size) {
        snapshotFlow {
            val layout = gridState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            layout.totalItemsCount > 0 && lastVisible >= layout.totalItemsCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnLoadMore() }
    }

    PullToRefreshBox(
        isRefreshing = feed.isRefreshing,
        onRefresh = actions.onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(adaptiveGridColumns()),
            state = gridState,
            contentPadding = adaptiveListPadding(PaddingValues(horizontal = 4.dp, vertical = 8.dp)),
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                feed.isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = XLiveColors.Accent)
                    }
                }
                feed.error != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        XLiveMessage(text = feed.error, onRetry = actions.onRetry)
                    }
                }
                feed.rooms.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        XLivePlaceholder(text = "Сейчас нет эфиров по этому отбору")
                    }
                }
                else -> {
                    items(items = feed.rooms, key = { it.username }) { room ->
                        XLiveRoomCard(item = room, onClick = { actions.onOpenRoom(room.username) })
                    }
                    if (feed.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = XLiveColors.Accent, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    if (feed.loadMoreError != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            XLiveLoadMoreError(message = feed.loadMoreError, onRetry = actions.onRetryLoadMore)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun XLiveRoomsGridPreview() {
    XLiveRoomsGrid(
        feed = XLiveFeedState(
            rooms = persistentListOf(
                XLiveRoomItem(username = "room_a", age = 21, country = "US", viewers = 63_200),
                XLiveRoomItem(username = "room_b", age = 24, viewers = 1_500, tags = listOf("new")),
            )
        ),
        gridState = rememberLazyGridState(),
        actions = XLiveFeedActions(),
    )
}
