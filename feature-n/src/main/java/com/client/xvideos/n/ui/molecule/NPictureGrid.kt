package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    onItemDelete: ((NPicture) -> Unit)? = null
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(adaptiveUserGridColumns(columns)),
        modifier = modifier,
        contentPadding = contentPadding,
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
            val handleDelete = remember(picture, onItemDelete) {
                if (onItemDelete != null) {
                    { onItemDelete(picture) }
                } else null
            }
            NPictureCard(
                picture = picture,
                onClick = handleClick,
                onDelete = handleDelete
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
