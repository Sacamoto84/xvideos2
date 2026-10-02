package com.client.xvideos.l.ui.screens.screenAlbumList.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.ui.screens.screenAlbumList.StatusAlbumList

/**
 * Страница пейджера списка альбомов: сетка [content] и поверх неё состояние
 * пустой страницы — спиннер, пока страница грузится, либо причина отказа с
 * «Повторить». Если на странице уже есть альбомы, поверх ничего не рисуется.
 *
 * Сетка и состояние обязаны лежать в одном `Box`: корневые узлы страницы
 * `HorizontalPager` раскладывает друг за другом по горизонтали
 * (`MeasuredPage.position`), и всё, что шло после сетки на всю ширину,
 * уезжало за край экрана — так спиннер загрузки не был виден никогда.
 *
 * @param status Статус загрузки страницы; `null` — страница ещё не запрашивалась.
 * @param isEmpty На странице нет альбомов.
 * @param errorMessage Текст ошибки при [StatusAlbumList.ERROR].
 * @param onRetry Повторная загрузка страницы.
 * @param content Сетка альбомов страницы.
 */
@Composable
fun AlbumListPageWithStatus(
    status: StatusAlbumList?,
    isEmpty: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        content()
        if (isEmpty) {
            when (status) {
                StatusAlbumList.DOWNLOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                StatusAlbumList.ERROR -> AlbumListPageError(message = errorMessage.orEmpty(), onRetry = onRetry)
                StatusAlbumList.BUSY, StatusAlbumList.DOWNLOADED, null -> Unit
            }
        }
    }
}

@Preview
@Composable
private fun AlbumListPageWithStatusPreview() {
    AlbumListPageWithStatus(
        status = StatusAlbumList.ERROR,
        isEmpty = true,
        errorMessage = "Сервер L недоступен (HTTP 500)",
        onRetry = {}
    ) {
        Text("сетка")
    }
}
