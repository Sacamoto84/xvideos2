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
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
internal fun LCollectionsTopBar(
    selectedCollection: String?,
    sortOrder: LCollectionSortOrder,
    onSortOrderClick: (LCollectionSortOrder) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column( modifier = Modifier  )
    {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                //.background(Theme.tabLevel1)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {

                if (!selectedCollection.isNullOrEmpty()) {
                    Text(
                        ">${selectedCollection}",
                        color = Theme.L.primaryColor,
                        fontSize = 18.sp,
                        fontFamily = Theme.L.fontFamilyPopinsRegular
                    )
                }

                if (selectedCollection == null) {
                    Text(
                        sortOrder.title,
                        color = Theme.L.grey2,
                        fontSize = 12.sp,
                        fontFamily = Theme.L.fontFamilyDMsanss
                    )
                }

            }

            if (selectedCollection == null) {

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Сортировка коллекций",
                            tint = Theme.L.textColor
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = Theme.L.grey3
                    ) {
                        LCollectionSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        order.title,
                                        style = Theme.L.Type.menuItem.copy(
                                            color = if (order == sortOrder) Color.White else Theme.L.grey2
                                        )
                                    )
                                },
                                onClick = {
                                    onSortOrderClick(order)
                                    menuExpanded = false
                                }
                            )
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

