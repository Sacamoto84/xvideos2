package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.collectionDB.model.CollectionsGridStyle
import com.composeunstyled.Text

/**
 * Сетка коллекций: список + заголовок ([topBar]) + кнопка «+».
 *
 * Показывает ТОЛЬКО список коллекций. Открытую коллекцию рендерит вызывающий
 * код отдельно (как соседний экран), а не вложенно сюда — поэтому здесь один
 * Scaffold и один topBar.
 */
@Composable
fun CollectionsGrid(
    collections: List<CollectionGridItem>,
    gridState: LazyGridState,
    style: CollectionsGridStyle,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (String) -> Unit,
    onCreateNewCollectionClick: () -> Unit,
    topBar: @Composable (() -> Unit)? = null
) {
    Scaffold(
        topBar = { topBar?.invoke() },
        containerColor = style.backgroundColor
    ) { padding ->
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = gridState,
            columns = GridCells.Fixed(2)
        ) {
            items(collections) { collection ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onClick = { onCollectionClick(collection.name) },
                            onLongClick = { onCollectionLongClick(collection.name) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewUrl = collection.previewUrl
                    if (previewUrl != null) {
                        UrlImage(
                            url = previewUrl,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .size(72.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .size(72.dp)
                                .background(style.placeholderColor)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            collection.name,
                            color = style.itemNameColor,
                            fontFamily = style.itemFontFamily
                        )
                        collection.itemsCount?.let { count ->
                            Text(
                                "Элементов: $count",
                                color = style.itemSecondaryColor,
                                fontSize = 12.sp,
                                fontFamily = style.itemFontFamily
                            )
                        }
                    }
                }
            }

            items(listOf(Unit)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp, top = 4.dp)
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(style.addButtonBackground)
                            .clickable(onClick = onCreateNewCollectionClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = style.addButtonIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// PREVIEW
//
// previewUrl = null у всех элементов → ветка серой заглушки (без сетевого
// UrlImage, который в @Preview не грузится). Стиль — фейковый, цвета хардкодом.
// ----------------------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFF262626, widthDp = 380, heightDp = 360)
@Composable
private fun CollectionsGridPreview() {
    CollectionsGrid(
        collections = listOf(
            CollectionGridItem(name = "Favorites", previewUrl = null, itemsCount = 42),
            CollectionGridItem(name = "Best gifs", previewUrl = null, itemsCount = 7),
            CollectionGridItem(name = "Без счётчика", previewUrl = null, itemsCount = null),
            CollectionGridItem(name = "Длинное название коллекции для проверки", previewUrl = null, itemsCount = 128),
        ),
        gridState = rememberLazyGridState(),
        style = CollectionsGridStyle(
            backgroundColor = Color(0xFF262626),
            titleColor = Color.White,
            titleFontFamily = FontFamily.Default,
            itemNameColor = Color.White,
            itemSecondaryColor = Color(0xFFB0B0B0),
            itemFontFamily = FontFamily.Default,
            addButtonBackground = Color(0xFF3A3A3A),
            addButtonIconColor = Color.White,
            placeholderColor = Color(0xFF4A4A4A),
        ),
        onCollectionClick = {},
        onCollectionLongClick = {},
        onCreateNewCollectionClick = {},
    )
}
