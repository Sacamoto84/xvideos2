package com.client.xvideos.x.screens.tags.molecule

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.tags.atom.TagGridCell
import com.client.xvideos.x.screens.tags.atom.TagsStateMessage
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Одна страница выдачи по тегу.
 *
 * Страницу грузит сама — так же, как `DashboardsPaginatedListScreen` в ленте
 * раздела: пейджер отдаёт только номер, а соседние страницы готовятся заранее
 * через `beyondViewportPageCount`.
 */
@Composable
fun TagsPaginatedListScreen(
    pageIndex: Int,
    loadPage: suspend (Int) -> List<ItemsX>,
    onOpenVideo: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    header: (@Composable () -> Unit)? = null,
    initialItems: List<ItemsX>? = null,
) {
    // Уже загруженная страница показывается сразу, без кадра с индикатором загрузки.
    var items by remember(pageIndex) { mutableStateOf(initialItems) }
    var failed by remember(pageIndex) { mutableStateOf(false) }
    var retryTrigger by remember(pageIndex) { mutableIntStateOf(0) }

    val loaded = items

    LaunchedEffect(pageIndex, retryTrigger) {
        failed = false
        try {
            items = loadPage(pageIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "!!! Страница тега %d не загрузилась", pageIndex)
            failed = true
        }
    }

    val onRetry: () -> Unit = remember(pageIndex) {
        {
            retryTrigger += 1
        }
    }

    if (loaded == null) {
        TagsStatusLayout(modifier = modifier, header = header) {
            if (failed) {
                TagsStateMessage(
                    message = "Страница не загрузилась",
                    onRetry = onRetry
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        }
        return
    }

    if (loaded.isEmpty()) {
        TagsStatusLayout(modifier = modifier, header = header) {
            TagsStateMessage(
                message = "Видео не найдены",
                onRetry = onRetry
            )
        }
        return
    }

    val orientation = LocalConfiguration.current.orientation
    val itemsPerRow = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
    val chunkedRows = remember(loaded, itemsPerRow) { loaded.chunked(itemsPerRow) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (header != null) {
            item(key = "tag_header", contentType = "tag_header") {
                header()
            }
        }
        itemsIndexed(
            items = chunkedRows,
            key = { index, row -> "${index}_${row.first().id}" },
            contentType = { _, _ -> "tag_row" }
        ) { _, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    key(cell.id) {
                        TagGridCell(
                            cell = cell,
                            onOpenVideo = onOpenVideo,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (row.size < itemsPerRow) {
                    repeat(itemsPerRow - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun TagsPaginatedListScreenPreview() {
    TagsPaginatedListScreen(
        pageIndex = 0,
        loadPage = {
            listOf(
                ItemsX(
                    id = 101L,
                    title = "Sample Tag Video 1",
                    duration = "08:15",
                    views = "250K",
                    channel = "Studio1",
                    href = "/video101",
                    nameProfile = "Studio1",
                    linkProfile = "/studio1",
                ),
                ItemsX(
                    id = 102L,
                    title = "Sample Tag Video 2",
                    duration = "14:20",
                    views = "500K",
                    channel = "Studio2",
                    href = "/video102",
                    nameProfile = "Studio2",
                    linkProfile = "/studio2",
                )
            )
        },
        onOpenVideo = {}
    )
}
