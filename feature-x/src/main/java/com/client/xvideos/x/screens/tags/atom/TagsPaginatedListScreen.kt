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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX

/**
 * Список видео по тегу.
 *
 * Раньше экран был заглушкой: список создавался пустым (`mutableStateListOf()`),
 * заполнявший его `LaunchedEffect` стоял закомментированным, а разобранные
 * `ScreenTagsViewModel`-ом элементы сюда не передавались вовсе. Заголовок и
 * счётчик результатов рисовались, список оставался пуст всегда — с первого
 * коммита.
 *
 * Постраничности здесь нет: `parserScreenTags` разбирает одну страницу выдачи,
 * и экран показывает ровно её. Прежний параметр `pageIndex` ни на что не влиял
 * (вызывался с жёстким `0`) и убран, чтобы не обещать несуществующего.
 */
@Composable
fun TagsPaginatedListScreen(
    items: List<ItemsX>,
    onOpenVideo: (ItemsX) -> Unit,
) {

    val itemsPerRow = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items.chunked(itemsPerRow))
        { row ->
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
