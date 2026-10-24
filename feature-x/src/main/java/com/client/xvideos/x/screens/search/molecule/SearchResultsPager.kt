package com.client.xvideos.x.screens.search.molecule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.x.feature.saved.SavedLocalVideo
import com.client.xvideos.x.model.ItemsX

/**
 * Выдача поиска, которая листается свайпом по страницам, как лента X.
 *
 * Страница просит загрузку, когда пейджер её показывает (соседняя — заранее);
 * загруженные хранит ScreenModel, и возврат к ним их не перезапрашивает.
 *
 * @param pages Загруженные страницы: номер → ролики.
 * @param failedPages Страницы, которые не загрузились.
 */
@Suppress("LongParameterList")
@Composable
fun SearchResultsPager(
    pagerState: PagerState,
    pages: Map<Int, List<ItemsX>>,
    loadingPages: Set<Int>,
    failedPages: Set<Int>,
    gridPosition: (Int) -> LazyGridPosition,
    onLoadPage: (Int) -> Unit,
    onRetryPage: (Int) -> Unit,
    isFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    openVideoPlayer: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
    localVideoOf: (ItemsX) -> SavedLocalVideo? = { null },
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { pageIndex -> pageIndex }
    ) { page ->
        val items = pages[page]
        val isFailed = page in failedPages
        val needsLoad = items == null && page !in loadingPages && !isFailed
        LaunchedEffect(page, needsLoad) {
            if (needsLoad) onLoadPage(page)
        }
        SearchResultsView(
            items = items,
            isError = isFailed && items == null,
            onRetry = { onRetryPage(page) },
            isFavorite = isFavorite,
            onFavoriteAdd = onFavoriteAdd,
            onFavoriteRemove = onFavoriteRemove,
            onDownload = onDownload,
            onSaveToGallery = onSaveToGallery,
            openVideoPlayer = openVideoPlayer,
            gridState = gridPosition(page).rememberState(),
            localVideoOf = localVideoOf,
        )
    }
}

@Preview
@Composable
private fun SearchResultsPagerPreview() {
    SearchResultsPager(
        pagerState = rememberPagerState { 3 },
        pages = mapOf(0 to emptyList()),
        loadingPages = emptySet(),
        failedPages = emptySet(),
        gridPosition = { LazyGridPosition() },
        onLoadPage = {},
        onRetryPage = {},
        isFavorite = { false },
        onFavoriteAdd = {},
        onFavoriteRemove = {},
        onDownload = {},
        onSaveToGallery = {},
        openVideoPlayer = {},
    )
}
