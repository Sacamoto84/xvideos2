package com.client.xvideos.xlive.ui.saved.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns
import com.client.xvideos.common.ui.adaptive.adaptiveListPadding
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.network.xLiveSnapshotUrl
import com.client.xvideos.xlive.ui.feed.atom.XLivePlaceholder
import com.client.xvideos.xlive.ui.saved.XLiveSavedActions
import com.client.xvideos.xlive.ui.saved.XLiveSavedState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * Сетка «Моих» или «Истории»: карточки с плашкой эфира, свайп вниз —
 * проверить эфир заново. Колонки те же, что у «Эфиров».
 *
 * Долгое нажатие на карточку спрашивает, убрать ли комнату. Какая комната
 * ждёт ответа — состояние самой сетки: после ухода с экрана диалог не нужен.
 *
 * @param emptyText подпись, пока список пуст.
 * @param listName список в родительном падеже — для диалога: «Моих», «истории».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XLiveSavedGrid(
    state: XLiveSavedState,
    gridState: LazyGridState,
    actions: XLiveSavedActions,
    emptyText: String,
    listName: String,
    modifier: Modifier = Modifier,
) {
    var pendingRemoval by remember { mutableStateOf<String?>(null) }

    PullToRefreshBox(
        isRefreshing = state.isChecking,
        onRefresh = actions.onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(adaptiveGridColumns()),
            state = gridState,
            contentPadding = adaptiveListPadding(PaddingValues(horizontal = 4.dp, vertical = 8.dp)),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.rooms.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        XLivePlaceholder(text = emptyText)
                    }
                }
            } else {
                items(items = state.rooms, key = { it.username }) { room ->
                    XLiveSavedRoomCard(
                        room = room,
                        thumbnail = xLiveSnapshotUrl(room.username, state.snapshotStamp),
                        online = state.online[room.username],
                        onClick = { actions.onOpenRoom(room.username) },
                        onLongClick = { pendingRemoval = room.username },
                    )
                }
            }
        }
    }

    pendingRemoval?.let { username ->
        XLiveRemoveRoomDialog(
            username = username,
            listName = listName,
            onDismiss = { pendingRemoval = null },
            onConfirm = {
                actions.onRemove(username)
                pendingRemoval = null
            },
        )
    }
}

@Preview
@Composable
private fun XLiveSavedGridPreview() {
    XLiveSavedGrid(
        state = XLiveSavedState(
            rooms = persistentListOf(XLiveSavedRoom("room_a", 24), XLiveSavedRoom("room_b")),
            online = persistentMapOf("room_a" to true, "room_b" to false),
        ),
        gridState = rememberLazyGridState(),
        actions = XLiveSavedActions(),
        emptyText = "Пусто",
        listName = "«Моих»",
    )
}
