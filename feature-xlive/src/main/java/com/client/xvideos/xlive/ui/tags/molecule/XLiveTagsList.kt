package com.client.xvideos.xlive.ui.tags.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveListPadding
import com.client.xvideos.xlive.model.XLiveTag
import com.client.xvideos.xlive.model.XLiveTagRoom
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.feed.atom.XLiveLoadMoreError
import com.client.xvideos.xlive.ui.feed.atom.XLivePlaceholder
import com.client.xvideos.xlive.ui.room.atom.XLiveMessage
import com.client.xvideos.xlive.ui.tags.XLiveTagsActions
import com.client.xvideos.xlive.ui.tags.XLiveTagsState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Догружать, когда до конца списка осталось столько тегов. */
private const val LOAD_MORE_THRESHOLD = 8

/**
 * Список вкладки «Теги»: строки тегов с превью комнат, догрузка в подвале.
 * Список на всю ширину окна, строки — не шире лимита по центру: свайп
 * работает и по краям раскрытого складного.
 */
@Composable
fun XLiveTagsList(
    state: XLiveTagsState,
    listState: LazyListState,
    actions: XLiveTagsActions,
    modifier: Modifier = Modifier,
) {
    val currentOnLoadMore by rememberUpdatedState(actions.onLoadMore)
    // Число тегов — в ключе: после догрузки условие проверяется заново.
    LaunchedEffect(listState, state.tags.size) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            layout.totalItemsCount > 0 && lastVisible >= layout.totalItemsCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnLoadMore() }
    }

    when {
        state.isLoading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = XLiveColors.Accent)
        }
        state.error != null -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            XLiveMessage(text = state.error, onRetry = actions.onRetry)
        }
        else -> LazyColumn(
            state = listState,
            contentPadding = adaptiveListPadding(PaddingValues(horizontal = 4.dp, vertical = 8.dp)),
            modifier = modifier.fillMaxSize(),
        ) {
            if (state.tags.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        XLivePlaceholder(text = "Тегов нет")
                    }
                }
            } else {
                items(items = state.tags, key = { it.name }) { tag ->
                    XLiveTagRow(
                        tag = tag,
                        onOpenTag = { actions.onOpenTag(tag.name) },
                        onOpenRoom = actions.onOpenRoom,
                    )
                }
                if (state.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = XLiveColors.Accent, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                if (state.loadMoreError != null) {
                    item {
                        XLiveLoadMoreError(message = state.loadMoreError, onRetry = actions.onRetryLoadMore)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun XLiveTagsListPreview() {
    XLiveTagsList(
        state = XLiveTagsState(
            tags = persistentListOf(
                XLiveTag("petite", 249_367, 1195, listOf(XLiveTagRoom("room_a", ""))),
                XLiveTag("asian", 2116, 189),
            )
        ),
        listState = rememberLazyListState(),
        actions = XLiveTagsActions(),
    )
}
