package com.client.xvideos.x.screens.tags.atom

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import kotlinx.coroutines.CancellationException
import timber.log.Timber

private const val TAG_CARD_ASPECT_RATIO = 352f / 198f
private const val ITEMS_PER_ROW_LANDSCAPE = 4
private const val ITEMS_PER_ROW_PORTRAIT = 2
private const val CONTENT_TYPE_TAG_HEADER = "tag_header"
private const val CONTENT_TYPE_TAG_ROW = "tag_row"
private const val MSG_FAILED_TO_LOAD = "Страница не загрузилась"
private const val MSG_NO_VIDEOS = "Видео не найдены"
private const val BUTTON_RETRY_TEXT = "Повторить"

/**
 * Одна страница выдачи по тегу.
 *
 * Страницу грузит сама — так же, как `DashboardsPaginatedListScreen` в ленте
 * раздела: пейджер отдаёт только номер, а соседние страницы готовятся заранее
 * через `beyondViewportPageCount`.
 *
 * Раньше экран был заглушкой: список создавался пустым, заполнявший его
 * `LaunchedEffect` стоял закомментированным, а разобранные элементы сюда не
 * передавались вовсе.
 */
@Composable
fun TagsPaginatedListScreen(
    pageIndex: Int,
    loadPage: suspend (Int) -> List<ItemsX>,
    onOpenVideo: (ItemsX) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    header: (@Composable () -> Unit)? = null,
) {

    var items by remember(pageIndex) { mutableStateOf<List<ItemsX>?>(null) }
    var failed by remember(pageIndex) { mutableStateOf(false) }
    var retryTrigger by remember(pageIndex) { mutableIntStateOf(0) }

    val loaded = items

    LaunchedEffect(pageIndex, retryTrigger) {
        // Отказ сети обязан оставаться на этом экране. Непойманное исключение в
        // корутине роняет приложение целиком, а страниц здесь грузится сразу
        // несколько: соседние готовятся заранее через beyondViewportPageCount.
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
        Column(modifier = Modifier.fillMaxSize()) {
            header?.invoke()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (failed) {
                    TagsStateMessage(
                        message = MSG_FAILED_TO_LOAD,
                        onRetry = onRetry
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                }
            }
        }
        return
    }

    if (loaded.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize()) {
            header?.invoke()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TagsStateMessage(
                    message = MSG_NO_VIDEOS,
                    onRetry = onRetry
                )
            }
        }
        return
    }

    val orientation = LocalConfiguration.current.orientation
    val itemsPerRow = remember(orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) ITEMS_PER_ROW_LANDSCAPE else ITEMS_PER_ROW_PORTRAIT
    }
    val chunkedRows = remember(loaded, itemsPerRow) { loaded.chunked(itemsPerRow) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (header != null) {
            item(key = CONTENT_TYPE_TAG_HEADER, contentType = CONTENT_TYPE_TAG_HEADER) {
                header()
            }
        }
        // Ключ с индексом, а не голый id: страницы тегов парсятся из HTML и один
        // и тот же ролик может встретиться на нескольких страницах — дублирующийся
        // ключ уронил бы список.
        itemsIndexed(
            items = chunkedRows,
            key = { index, row -> "${index}_${row.first().id}" },
            contentType = { _, _ -> CONTENT_TYPE_TAG_ROW }
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
                // Если элементов в строке меньше, чем itemsPerRow, добавляем пустые ячейки
                if (row.size < itemsPerRow) {
                    repeat(itemsPerRow - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TagGridCell(
    cell: ItemsX,
    onOpenVideo: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(cell, onOpenVideo) { { onOpenVideo(cell) } }
    Box(
        modifier = modifier
            .aspectRatio(TAG_CARD_ASPECT_RATIO)
            .padding(1.dp)
            .background(Color.DarkGray)
    ) {
        // Жесты как в ленте раздела: тап — превью, долгий тап и
        // двойной — открыть плеер.
        UrlVideoImageAndLongClickX(
            cell,
            onLongClick = handleOpen,
            onDoubleClick = handleOpen,
        )
    }
}

@Composable
private fun TagsStateMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(BUTTON_RETRY_TEXT)
        }
    }
}
