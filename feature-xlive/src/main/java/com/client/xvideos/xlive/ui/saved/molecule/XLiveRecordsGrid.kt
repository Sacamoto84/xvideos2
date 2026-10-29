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
import com.client.xvideos.xlive.model.XLiveRecord
import com.client.xvideos.xlive.model.XLiveRecordStatus
import com.client.xvideos.xlive.ui.feed.atom.XLivePlaceholder
import com.client.xvideos.xlive.ui.saved.XLiveRecordsActions
import com.client.xvideos.xlive.ui.saved.XLiveRecordsState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import java.io.File

/**
 * Сетка «Записей»: карточки от новых к старым, колонки те же, что у «Эфиров».
 *
 * Долгое нажатие спрашивает, удалить ли запись; какая запись ждёт ответа —
 * состояние самой сетки, как у «Моих».
 */
@Composable
fun XLiveRecordsGrid(
    state: XLiveRecordsState,
    gridState: LazyGridState,
    actions: XLiveRecordsActions,
    modifier: Modifier = Modifier,
) {
    var pendingRemoval by remember { mutableStateOf<Long?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(adaptiveGridColumns()),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        if (state.records.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    XLivePlaceholder(text = "Здесь будут записи эфиров — кнопка записи в шапке комнаты")
                }
            }
        } else {
            items(items = state.records, key = { it.id }) { record ->
                XLiveRecordCard(
                    record = record,
                    snapshot = state.snapshots[record.id]?.let(::File),
                    onClick = { actions.onOpen(record.id) },
                    onLongClick = { pendingRemoval = record.id },
                    online = state.online[record.username] == true,
                    // В галерею — только готовая запись: у пишущейся и оборванной видео ещё нет.
                    onSaveToGallery = if (record.status == XLiveRecordStatus.READY) {
                        { actions.onSaveToGallery(record.id) }
                    } else {
                        null
                    },
                )
            }
        }
    }

    pendingRemoval?.let { id ->
        XLiveRemoveRecordDialog(
            onDismiss = { pendingRemoval = null },
            onConfirm = {
                actions.onRemove(id)
                pendingRemoval = null
            },
        )
    }
}

@Preview
@Composable
private fun XLiveRecordsGridPreview() {
    XLiveRecordsGrid(
        state = XLiveRecordsState(
            records = persistentListOf(
                XLiveRecord(id = 2L, username = "room_a", status = XLiveRecordStatus.READY, durationMs = 65_000L),
                XLiveRecord(id = 1L, username = "room_b", status = XLiveRecordStatus.BROKEN),
            ),
            online = persistentMapOf("room_a" to true),
        ),
        gridState = rememberLazyGridState(),
        actions = XLiveRecordsActions(),
    )
}
