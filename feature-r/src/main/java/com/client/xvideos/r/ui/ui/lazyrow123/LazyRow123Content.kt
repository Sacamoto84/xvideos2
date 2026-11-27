package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.navigator.LocalNavigator
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.explorer.LocalRNavigationState
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreen
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import timber.log.Timber

/** @param columns колонки сетки, уже с запасом широкого окна — их считает [LazyRow123]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyRow123Content(
    host: LazyRow123Host,
    listGifs: LazyPagingItems<GifsInfo>,
    columns: Int,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    onClickOpenProfile: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentBeforeList: (@Composable () -> Unit)? = null,
    isRunLike: Boolean = false,
    onAppendLoaded: (LazyPagingItems<GifsInfo>) -> Unit = {},
) {
    SideEffect { Timber.d("!!! LazyRow123::SideEffect columns: ${host.columns}") }

    val isConnected by host.isConnected.collectAsStateWithLifecycle()
    val state = gridState

    val navigator = LocalNavigator.current
    val navigationState = LocalRNavigationState.current
    val downloadList by host.downloadRed.downloadList.collectAsStateWithLifecycle()
    val downloadedIds = remember(downloadList) { downloadList.mapTo(HashSet(downloadList.size)) { it.id } }
    val loadState = listGifs.loadState
    var wasAppendLoading by remember { mutableStateOf(false) }
    val currentOnAppendLoaded by rememberUpdatedState(onAppendLoaded)

    LaunchedEffect(loadState.refresh) {
        if (loadState.refresh is LoadState.NotLoading) {
            currentOnAppendLoaded(listGifs)
        }
    }

    LaunchedEffect(loadState.append) {
        if (loadState.append is LoadState.Loading && !wasAppendLoading) {
            wasAppendLoading = true
            currentOnAppendLoaded(listGifs)
        }
        if (loadState.append !is LoadState.Loading) {
            wasAppendLoading = false
        }
    }

    LaunchedEffect(host.returnToIndex, listGifs.itemCount, host.columns) {
        val targetIndex = host.returnToIndex
        if (targetIndex >= 0 && listGifs.itemCount > targetIndex) {
            val offset = if (contentBeforeList != null) 1 else 0
            state.scrollToItem(targetIndex + offset)
            host.returnToIndex = -1
        }
    }

    // paging-compose itemKey зовёт peek(index) и кидает IndexOutOfBounds, когда снапшот
    // синхронно сжался (unlike в R Likes), а грид ещё запрашивает ключи по старому
    // диапазону. Защищаем границу: за пределами снапшота отдаём индекс-заглушку.
    val pagingItemKey = listGifs.itemKey { it.id }

    LazyRow123ContentStateless(
        columns = columns,
        state = state,
        itemCount = listGifs.itemCount,
        loadState = loadState,
        itemKey = { index -> if (index < listGifs.itemCount) "${pagingItemKey(index)}#$index" else index },
        modifier = modifier,
        contentPadding = contentPadding,
        contentBeforeList = contentBeforeList,
        onRetry = { listGifs.retry() },
        showLoadingWhenEmpty = when (host.typePager) {
            TypePager.NICHES, TypePager.PROFILE -> false
            else -> true
        },
    ) { index ->
        // Та же защита границы, что и для itemKey (см. выше).
        if (index >= listGifs.itemCount) return@LazyRow123ContentStateless
        listGifs[index]?.let { item ->
            val isDownloaded = item.id in downloadedIds

            LazyRow123GridItem(
                item = item,
                index = index,
                host = host,
                isConnected = isConnected,
                isDownloaded = isDownloaded,
                isRunLike = isRunLike,
                onItemClick = {
                    host.currentIndex = index
                    host.returnToIndex = index
                    host.block.blockItem = item
                    navigator?.push(ScreenRedFullScreen(item, host.feedKey, index))
                },
                onRefresh = { listGifs.refresh() },
                onClickOpenProfile = onClickOpenProfile,
                onTagClick = { tag ->
                    host.search.searchText.value = TextFieldValue(tag, TextRange(tag.length))
                    host.search.searchTextDone.value = tag
                    navigationState.rootTab = 0
                    navigator?.popAll()
                }
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}
