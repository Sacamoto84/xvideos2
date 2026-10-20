package com.client.xvideos.x.screens.saved.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.saved.atom.SavedHeader

/**
 * Сетка скачанных роликов с шапкой: две колонки на телефоне, больше на широком
 * окне — как у «Избранного». Пустой список — надпись «Пусто».
 */
@Composable
fun X_SavedList(
    list: List<ItemsX>,
    topCutout: androidx.compose.ui.unit.Dp,
    gridState: LazyGridState = rememberLazyGridState(),
    onPlayItem: (ItemsX) -> Unit = {},
    onDeleteItem: (ItemsX) -> Unit = {},
    onShareP2pItem: (ItemsX) -> Unit = {},
    posterUrlProvider: (ItemsX) -> String = { it.previewImage },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.L.grey6)
    ) {
        if (list.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SavedHeader(topCutout = topCutout)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Пусто", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(adaptiveGridColumns()),
                state = gridState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Шапка — на всю строку сетки, как у «Избранного».
                item(key = "header", contentType = "header", span = { GridItemSpan(maxLineSpan) }) {
                    SavedHeader(topCutout = topCutout)
                }
                items(
                    items = list,
                    key = { it.id },
                    contentType = { "saved_row" }
                ) { item ->
                    SavedRow(
                        item = item,
                        posterUrl = posterUrlProvider(item),
                        onPlay = onPlayItem,
                        onDelete = onDeleteItem,
                        onShareP2p = onShareP2pItem,
                        modifier = Modifier.padding(horizontal = 1.dp),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun X_SavedListPreview() {
    X_SavedList(
        list = emptyList(),
        topCutout = 0.dp
    )
}
