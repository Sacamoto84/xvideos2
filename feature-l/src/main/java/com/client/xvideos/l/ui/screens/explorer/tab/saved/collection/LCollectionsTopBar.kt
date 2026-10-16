package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
import com.client.xvideos.ui.theme.XvideosTheme



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
        selectedCollection?.let { ">$it" }
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

    // Ряд — не шире 640dp по центру, разделитель под ним — на всю ширину.
    Column(
        modifier = columnModifier
    ) {
        Row(
            modifier = Modifier
                .adaptiveContentWidth()
                .fillMaxWidth()
                // Кнопка сортировки даёт поле 12dp вокруг значка: значок — на
                // 16dp от края, как стрелка «назад» в шапках.
                .padding(start = if (selectedCollection == null) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Значок и выбранный порядок — одна кнопка: меню открывает нажатие
            // и на значок, и на текст.
            if (selectedCollection == null) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onOpenMenu)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Сортировка коллекций",
                            tint = Theme.L.textColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            sortOrder.title,
                            color = Theme.L.grey2,
                            fontSize = 16.sp,
                            fontFamily = Theme.L.fontFamilyDMsanss,
                            maxLines = 1
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
            Column(modifier = Modifier.weight(1f)) {
                if (collectionTitle != null) {
                    Text(
                        collectionTitle,
                        color = Theme.L.primaryColor,
                        fontSize = 18.sp,
                        fontFamily = Theme.L.fontFamilyPopinsRegular
                    )
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

