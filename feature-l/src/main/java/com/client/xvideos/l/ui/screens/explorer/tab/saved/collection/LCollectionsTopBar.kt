package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
import com.client.xvideos.ui.theme.XvideosTheme

private val BAR_START_PADDING = 8.dp
private val COLLECTION_TITLE_FONT_SIZE = 18.sp
private val SORT_ORDER_FONT_SIZE = 12.sp
private const val CD_COLLECTION_SORT = "Сортировка коллекций"
private const val COLLECTION_TITLE_PREFIX = ">"
private val ROW_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(start = BAR_START_PADDING)

@Composable
internal fun LCollectionsTopBar(
    selectedCollection: String?,
    sortOrder: LCollectionSortOrder,
    onSortOrderClick: (LCollectionSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val onOpenMenu = remember { { menuExpanded = true } }
    val onDismissMenu = remember { { menuExpanded = false } }
    val collectionTitle = remember(selectedCollection) {
        selectedCollection?.let { "$COLLECTION_TITLE_PREFIX$it" }
    }

    // Топ-бар лежит в Scaffold(topBar = ...) — Material3 не применяет инсет
    // к этому слоту, а хост таба паддит только низ. Соседние таби (Albums,
    // Likes) отступают от выреза сами; этот бар при миграции на Material3
    // потерял displayCutoutPadding и рендерился от y=0, под камерой.
    val topInset = getTopInsetDp()
    val columnModifier = if (modifier == Modifier) {
        Modifier
            .fillMaxWidth()
            .padding(top = topInset)
    } else {
        modifier
            .fillMaxWidth()
            .padding(top = topInset)
    }

    Column(
        modifier = columnModifier
    ) {
        Row(
            modifier = ROW_BASE_MODIFIER,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (collectionTitle != null) {
                    Text(
                        collectionTitle,
                        color = Theme.L.primaryColor,
                        fontSize = COLLECTION_TITLE_FONT_SIZE,
                        fontFamily = Theme.L.fontFamilyPopinsRegular
                    )
                }

                if (selectedCollection == null) {
                    Text(
                        sortOrder.title,
                        color = Theme.L.grey2,
                        fontSize = SORT_ORDER_FONT_SIZE,
                        fontFamily = Theme.L.fontFamilyDMsanss
                    )
                }
            }

            if (selectedCollection == null) {
                Box {
                    IconButton(onClick = onOpenMenu) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = CD_COLLECTION_SORT,
                            tint = Theme.L.textColor
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = onDismissMenu,
                        containerColor = Theme.L.grey3
                    ) {
                        val baseMenuItemStyle = Theme.L.Type.menuItem
                        val selectedMenuItemStyle = remember(baseMenuItemStyle) {
                            baseMenuItemStyle.copy(color = Color.White)
                        }
                        val unselectedMenuItemStyle = remember(baseMenuItemStyle) {
                            baseMenuItemStyle.copy(color = Theme.L.grey2)
                        }
                        LCollectionSortOrder.entries.forEach { order ->
                            key(order.name) {
                                val isSelected = order == sortOrder
                                val menuItemStyle = if (isSelected) selectedMenuItemStyle else unselectedMenuItemStyle
                                val handleOrderClick = remember(order, onSortOrderClick) {
                                    {
                                        onSortOrderClick(order)
                                        menuExpanded = false
                                    }
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            order.title,
                                            style = menuItemStyle
                                        )
                                    },
                                    onClick = handleOrderClick
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PreviewLCollectionsTopBarSelectionNull() {
    XvideosTheme(darkTheme = true) {
        LCollectionsTopBar(
            selectedCollection = null,
            sortOrder = LCollectionSortOrder.RECENT,
            onSortOrderClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PreviewLCollectionsTopBarWithSelection() {
    XvideosTheme(darkTheme = true) {
        LCollectionsTopBar(
            selectedCollection = "My Private Collection",
            sortOrder = LCollectionSortOrder.NAME,
            onSortOrderClick = {},
        )
    }
}

