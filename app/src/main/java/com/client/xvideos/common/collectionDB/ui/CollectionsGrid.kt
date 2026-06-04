package com.client.xvideos.common.collectionDB.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.composeunstyled.Text

/**
 * Универсальное представление одной коллекции в гриде (общее для L и R).
 *
 * @param name             отображаемое имя коллекции (оно же id папки)
 * @param previewUrl       url или локальный путь к превью; null — серая заглушка
 * @param itemsCount       подпись со счётчиком; null — счётчик не показывать
 */
data class CollectionGridItem(
    val name: String,
    val previewUrl: String?,
    val itemsCount: Int?
)

/**
 * Цветовая и типографическая тема для [CollectionsGrid]. Заполняется на стороне
 * каждого раздела (см. `ThemeL.toCollectionsGridStyle()` / аналог для R).
 */
data class CollectionsGridStyle(
    val backgroundColor: Color,
    val titleColor: Color,
    val titleFontFamily: FontFamily,
    val itemNameColor: Color,
    val itemSecondaryColor: Color,
    val itemFontFamily: FontFamily,
    val addButtonBackground: Color,
    val addButtonIconColor: Color = Color.Black,
    val placeholderColor: Color = Color.Gray
)

/**
 * Универсальная сетка коллекций c заголовком и кнопкой «+».
 *
 * Если [selectedCollection] не null, вместо сетки рендерится [navigationContent]
 * (контент конкретной коллекции — список её элементов).
 */
@Composable
fun CollectionsGrid(
    selectedCollection: String?,
    collections: List<CollectionGridItem>,
    gridState: LazyGridState,
    style: CollectionsGridStyle,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (String) -> Unit,
    onCreateNewCollectionClick: () -> Unit,
    topBar: @Composable (() -> Unit)? = null,
    navigationContent: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            if (topBar != null) {
                topBar()
            } else {
                Text(
                    ">Коллекция>$selectedCollection",
                    modifier = Modifier.padding(start = 8.dp),
                    color = style.titleColor,
                    fontSize = 18.sp,
                    fontFamily = style.titleFontFamily
                )
            }
        },
        containerColor = style.backgroundColor
    ) { padding ->
        if (selectedCollection == null) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = gridState,
                columns = GridCells.Fixed(2)
            ) {
                items(collections) { collection ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onClick = { onCollectionClick(collection.name) },
                                onLongClick = { onCollectionLongClick(collection.name) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (collection.previewUrl != null) {
                            UrlImage(
                                url = collection.previewUrl,
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
        } else {
            navigationContent()
        }
    }
}
