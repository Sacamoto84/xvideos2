package com.client.xvideos.common.ui.scroll

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier

/** Страницы альбомов L и N. Индексы пейджера и колбэков начинаются с нуля. */
@Composable
fun AlbumListPager(
    state: PagerState,
    onLoadPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    val loader = rememberUpdatedState(onLoadPage)
    val page = state.currentPage
    val count = state.pageCount
    LaunchedEffect(state, page, count) {
        if (page !in 0 until count) return@LaunchedEffect
        loader.value(page)
        for (neighbor in (page - 1).coerceAtLeast(0)..(page + 2).coerceAtMost(count - 1)) {
            if (neighbor != page) loader.value(neighbor)
        }
    }

    HorizontalPager(
        state = state,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { it }
    ) { content(it) }
}
