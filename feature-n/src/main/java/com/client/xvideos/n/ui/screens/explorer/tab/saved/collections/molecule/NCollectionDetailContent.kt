package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.molecule.NPictureGrid
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.atom.NCollectionTopBar

/**
 * Содержимое выбранной коллекции манги: верхняя панель и сетка картинок (или заглушка при пустоте).
 */
@Composable
fun NCollectionDetailContent(
    collectionName: String,
    items: List<NPicture>,
    columns: Int,
    topInset: Dp,
    onBack: () -> Unit,
    onRenameCollection: () -> Unit,
    onDeleteCollection: () -> Unit,
    onPictureClick: (NPicture) -> Unit,
    pictureMenuContent: @Composable (NPicture) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        NCollectionTopBar(
            collectionName = collectionName,
            itemsCount = items.size,
            onBack = onBack,
            onRenameCollection = onRenameCollection,
            onDeleteCollection = onDeleteCollection,
            topInset = topInset
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "В этой коллекции пока нет картинок",
                    color = Color(0xFFAAAAAA),
                    fontSize = 15.sp
                )
            }
        } else {
            NPictureGrid(
                items = items,
                columns = columns,
                onItemClick = onPictureClick,
                gridState = gridState,
                menuContent = pictureMenuContent,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview
@Composable
private fun NCollectionDetailContentPreview() {
    NCollectionDetailContent(
        collectionName = "Моя коллекция",
        items = emptyList(),
        columns = 2,
        topInset = 0.dp,
        onBack = {},
        onRenameCollection = {},
        onDeleteCollection = {},
        onPictureClick = {},
        pictureMenuContent = {}
    )
}
