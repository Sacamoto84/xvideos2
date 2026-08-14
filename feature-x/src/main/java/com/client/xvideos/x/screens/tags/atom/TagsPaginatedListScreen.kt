package com.client.xvideos.x.screens.tags.atom

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
) {

    var items by remember(pageIndex) { mutableStateOf<List<ItemsX>?>(null) }
    var failed by remember(pageIndex) { mutableStateOf(false) }

    LaunchedEffect(pageIndex) {
        // Отказ сети обязан оставаться на этом экране. Непойманное исключение в
        // корутине роняет приложение целиком, а страниц здесь грузится сразу
        // несколько: соседние готовятся заранее через beyondViewportPageCount.
        try {
            items = loadPage(pageIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "!!! Страница тега %d не загрузилась", pageIndex)
            failed = true
        }
    }

    val loaded = items
    if (loaded == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (failed) {
                Text("Страница не загрузилась", color = Color.Gray)
            } else {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        }
        return
    }

    val itemsPerRow = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ключ с индексом, а не голый id: страницы тегов парсятся из HTML и один
        // и тот же ролик может встретиться на нескольких страницах — дублирующийся
        // ключ уронил бы список.
        itemsIndexed(loaded.chunked(itemsPerRow), key = { index, row -> "${index}_${row.first().id}" })
        { _, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { index, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(352f / 198f)
                            .padding(1.dp)
                            .background(Color.DarkGray)
                    ) {

                        // Жесты как в ленте раздела: тап — превью, долгий тап и
                        // двойной — открыть плеер.
                        UrlVideoImageAndLongClickX(
                            cell,
                            onLongClick = { onOpenVideo(cell) },
                            onDoubleClick = { onOpenVideo(cell) },
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
