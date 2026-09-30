package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.collectionDB.model.CollectionsGridStyle
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.featured.saved.LCollectionEntity
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.CollectionsGrid
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionsTopBar
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun L_SavedCollectionTabContent(
    collectionList: List<LCollectionEntity>,
    sortOrder: LCollectionSortOrder,
    gridState: LazyGridState,
    onSortOrderClick: (LCollectionSortOrder) -> Unit,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (String) -> Unit,
    onCreateNewCollectionClick: () -> Unit
) {
    val gridItems = remember(collectionList) {
        collectionList.map {
            CollectionGridItem(
                name = it.collection,
                previewUrl = it.previewUrl,
                itemsCount = it.itemsCount
            )
        }
    }
    val gridStyle = remember {
        CollectionsGridStyle(
            backgroundColor = Theme.background,
            titleColor = Theme.L.primaryColor,
            titleFontFamily = Theme.L.fontFamilyPopinsRegular,
            itemNameColor = Color.White,
            itemSecondaryColor = Color.LightGray,
            itemFontFamily = Theme.L.fontFamilyDMsanss,
            addButtonBackground = Theme.L.primaryColor
        )
    }
    val topBarContent: @Composable () -> Unit = remember(sortOrder, onSortOrderClick) {
        {
            LCollectionsTopBar(
                selectedCollection = null,
                sortOrder = sortOrder,
                onSortOrderClick = onSortOrderClick,
            )
        }
    }
    CollectionsGrid(
        collections = gridItems,
        gridState = gridState,
        style = gridStyle,
        onCollectionClick = onCollectionClick,
        onCollectionLongClick = onCollectionLongClick,
        onCreateNewCollectionClick = onCreateNewCollectionClick,
        topBar = topBarContent
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun L_SavedCollectionTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        L_SavedCollectionTabContent(
            collectionList = listOf(
                LCollectionEntity("Favorites", null, 10, 0, 0, false),
                LCollectionEntity("Travel", null, 5, 0, 0, false),
                LCollectionEntity("Work", null, 2, 0, 0, false)
            ),
            sortOrder = LCollectionSortOrder.RECENT,
            gridState = rememberLazyGridState(),
            onSortOrderClick = {},
            onCollectionClick = {},
            onCollectionLongClick = {},
            onCreateNewCollectionClick = {}
        )
    }
}
