package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveListPadding
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.ui.atom.NGalleryCard

/**
 * Сетка карточек манги / галерей.
 */
@Composable
fun NGalleryGrid(
    items: List<NGalleryPreview>,
    columns: Int,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    onItemLongClick: ((NGalleryPreview) -> Unit)? = null,
    gridState: LazyGridState = rememberLazyGridState()
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(adaptiveUserGridColumns(columns)),
        modifier = modifier,
        contentPadding = adaptiveListPadding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            NGalleryCard(
                item = item,
                onClick = { onItemClick(item.id) },
                onLongClick = onItemLongClick?.let { callback -> { callback(item) } }
            )
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
