package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.TopLoadingBar
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.toRUserMessage
import com.client.xvideos.r.ui.ui.lazyrow123.molecule.RListLoadError

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyRow123ContentStateless(
    columns: Int,
    state: LazyGridState,
    itemCount: Int,
    loadState: CombinedLoadStates,
    itemKey: ((Int) -> Any)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentBeforeList: (@Composable () -> Unit)? = null,
    onRetry: () -> Unit = {},
    showLoadingWhenEmpty: Boolean = true,
    itemContent: @Composable (Int) -> Unit
) {
    val showLoadingIndicators = showLoadingWhenEmpty || itemCount > 0
    val isAnyLoading = loadState.refresh is LoadState.Loading ||
            loadState.append is LoadState.Loading ||
            loadState.prepend is LoadState.Loading

    Box(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            horizontalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
            verticalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
            state = state,
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            if (contentBeforeList != null) {
                item(key = "before", span = { GridItemSpan(maxLineSpan) }) { contentBeforeList() }
            }

            items(
                count = itemCount,
                key = itemKey,
                contentType = { "video_grid_item" }
            ) { index ->
                itemContent(index)
            }

            if (showLoadingIndicators && loadState.append is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            val loadError = listLoadError(loadState)
            if (loadError != null) {
                item(key = "load_error", span = { GridItemSpan(maxLineSpan) }) {
                    RListLoadError(message = loadError.toRUserMessage(), onRetry = onRetry)
                }
            }
        }

        if (showLoadingWhenEmpty && loadState.refresh is LoadState.Loading && itemCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (showLoadingIndicators && isAnyLoading) {
            TopLoadingBar(modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LazyRow123ContentStatelessPreview() {
    val sampleItems = List(10) { index ->
        GifsInfo(
            id = index.toString(),
            userName = "User $index",
            description = "Sample description $index"
        )
    }

    val notLoading = LoadState.NotLoading(false)
    val mockLoadState = CombinedLoadStates(
        refresh = notLoading,
        prepend = notLoading,
        append = notLoading,
        source = LoadStates(
            refresh = notLoading,
            prepend = notLoading,
            append = notLoading
        )
    )

    LazyRow123ContentStateless(
        columns = 2,
        state = rememberLazyGridState(),
        itemCount = sampleItems.size,
        loadState = mockLoadState,
        itemKey = { index -> sampleItems[index].id },
        contentPadding = PaddingValues(8.dp)
    ) { index ->
        Box(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Item $index", color = Color.White)
        }
    }
}
