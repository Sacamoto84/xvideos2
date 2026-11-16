package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.ui.atom.NGalleryCard

/**
 * Сетка карточек манги / галерей с поддержкой бесконечной прокрутки.
 */
@Composable
fun NGalleryGrid(
    items: List<NGalleryPreview>,
    columns: Int,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null
) {
    val gridState = rememberLazyGridState()

    if (onLoadMore != null) {
        val shouldLoadMore = remember(items.size, isLoadingMore) {
            derivedStateOf {
                val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                !isLoadingMore && items.isNotEmpty() && lastVisibleIndex >= items.size - 4
            }
        }

        LaunchedEffect(shouldLoadMore.value) {
            if (shouldLoadMore.value) {
                onLoadMore()
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(adaptiveUserGridColumns(columns)),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            NGalleryCard(
                item = item,
                onClick = { onItemClick(item.id) }
            )
        }

        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFED2553),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NGalleryGridPreview() {
    NGalleryGrid(
        items = listOf(
            NGalleryPreview(id = 1L, title = "Manga 1", numPages = 20),
            NGalleryPreview(id = 2L, title = "Manga 2", numPages = 35)
        ),
        columns = 2,
        onItemClick = {}
    )
}
