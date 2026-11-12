package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    contentPadding: PaddingValues = PaddingValues(8.dp)
) {
    LazyVerticalGrid(
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
