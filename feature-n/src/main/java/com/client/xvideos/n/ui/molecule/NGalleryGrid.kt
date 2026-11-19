package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.padding
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
    itemMenuActions: (@Composable (NGalleryPreview, dismiss: () -> Unit) -> Unit)? = null,
    gridState: LazyGridState = rememberLazyGridState()
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(adaptiveUserGridColumns(columns)),
        modifier = modifier,
        contentPadding = adaptiveListPadding()
    ) {
        items(items, key = { it.id }) { item ->
            NGalleryCard(
                item = item,
                modifier = Modifier.padding(2.dp),
                onClick = { onItemClick(item.id) },
                menuActions = if (itemMenuActions == null) null else { dismiss -> itemMenuActions(item, dismiss) }
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
