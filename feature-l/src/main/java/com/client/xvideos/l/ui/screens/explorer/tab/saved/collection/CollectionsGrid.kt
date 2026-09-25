package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

private val PREVIEW_CORNER = 8.dp
private val PREVIEW_CORNER_SHAPE = RoundedCornerShape(PREVIEW_CORNER)
private val ADD_BUTTON_PADDING_START = 8.dp
private val ADD_BUTTON_PADDING_TOP = 4.dp
private val ADD_BUTTON_SIZE = 72.dp
private val ADD_ICON_SIZE = 24.dp
private val CELL_HORIZONTAL_PADDING = 8.dp
private val CELL_VERTICAL_PADDING = 4.dp
private val PREVIEW_SIZE = 72.dp
private val TEXT_SPACER_WIDTH = 8.dp
private val COUNT_FONT_SIZE = 12.sp
private const val GRID_COLUMNS = 2

private const val CONTENT_TYPE_COLLECTION_ITEM = "collection_item"
private const val KEY_ADD_BUTTON = "add_button"
private const val CONTENT_TYPE_ADD_BUTTON = "add_button"
private const val COUNT_PREFIX = "Элементов: "
private val ZERO_WINDOW_INSETS = WindowInsets(0, 0, 0, 0)

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
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = ZERO_WINDOW_INSETS,
        topBar = { topBar?.invoke() },
        containerColor = style.backgroundColor
    ) { padding ->
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = gridState,
            columns = GridCells.Fixed(GRID_COLUMNS)
        ) {
            itemsIndexed(
                items = collections,
                key = { index, item -> "${item.name}#$index" },
                contentType = { _, _ -> CONTENT_TYPE_COLLECTION_ITEM }
            ) { _, collection ->
                CollectionGridCell(
                    collection = collection,
                    style = style,
                    shape = PREVIEW_CORNER_SHAPE,
                    onClick = onCollectionClick,
                    onLongClick = onCollectionLongClick
                )
            }

            item(key = KEY_ADD_BUTTON, contentType = CONTENT_TYPE_ADD_BUTTON) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .padding(start = ADD_BUTTON_PADDING_START, top = ADD_BUTTON_PADDING_TOP)
                            .size(ADD_BUTTON_SIZE)
                            .clip(PREVIEW_CORNER_SHAPE)
                            .background(style.addButtonBackground)
                            .clickable(onClick = onCreateNewCollectionClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = style.addButtonIconColor,
                            modifier = Modifier.size(ADD_ICON_SIZE)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionGridCell(
    collection: CollectionGridItem,
    style: CollectionsGridStyle,
    shape: RoundedCornerShape,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleClick = remember(collection.name, onClick) { { onClick(collection.name) } }
    val handleLongClick = remember(collection.name, onLongClick) { { onLongClick(collection.name) } }
    val countText = remember(collection.itemsCount) {
        collection.itemsCount?.let { "$COUNT_PREFIX$it" }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CELL_HORIZONTAL_PADDING, vertical = CELL_VERTICAL_PADDING)
            .combinedClickable(
                onClick = handleClick,
                onLongClick = handleLongClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val previewUrl = collection.previewUrl
        if (previewUrl != null) {
            UrlImage(
                url = previewUrl,
                modifier = Modifier
                    .clip(shape)
                    .size(PREVIEW_SIZE)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .size(PREVIEW_SIZE)
                    .background(style.placeholderColor)
            )
        }
        Spacer(Modifier.width(TEXT_SPACER_WIDTH))
        Column {
            Text(
                collection.name,
                color = style.itemNameColor,
                fontFamily = style.itemFontFamily
            )
            if (countText != null) {
                Text(
                    countText,
                    color = style.itemSecondaryColor,
                    fontSize = COUNT_FONT_SIZE,
                    fontFamily = style.itemFontFamily
                )
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
