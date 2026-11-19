package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveListPadding
import com.client.xvideos.common.ui.adaptive.adaptiveUserGridColumns
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.atom.NPictureCard

/**
 * Сетка карточек картинок / страниц манги.
 */
@Composable
fun NPictureGrid(
    items: List<NPicture>,
    columns: Int,
    onItemClick: (NPicture) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    menuContent: (@Composable (NPicture) -> Unit)? = null,
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
        items(
            items = items,
            key = { it.id }
        ) { picture ->
            val handleClick = remember(picture, onItemClick) {
                { onItemClick(picture) }
            }
            NPictureCard(
                picture = picture,
                onClick = handleClick,
                menuContent = menuContent?.let { content ->
                    { content(picture) }
                }
            )
        }
    }
}

@Preview
@Composable
private fun NPictureGridPreview() {
    NPictureGrid(
        items = listOf(
            NPicture(
                id = "1_1",
                galleryId = 1L,
                mediaId = 1L,
                pageNumber = 1,
                galleryTitle = "Title 1"
            ),
            NPicture(
                id = "1_2",
                galleryId = 1L,
                mediaId = 1L,
                pageNumber = 2,
                galleryTitle = "Title 1"
            )
        ),
        columns = 2,
        onItemClick = {}
    )
}
