package com.client.xvideos.r.ui.niche

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.common.ui.atom.TabBarPoints
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.collections.immutable.persistentListOf

private val NICHE_SORT_ORDERS = persistentListOf(Order.TRENDING, Order.TOP, Order.LATEST)
private val BAR_HEIGHT = 48.dp
private val INDICATOR_CONTAINER_SIZE = 44.dp
private val INDICATOR_BORDER_WIDTH = 1.dp
private val INDICATOR_BORDER_COLOR = Color.DarkGray
private val INNER_HORIZONTAL_SPACER = 4.dp
private val EDGE_HORIZONTAL_SPACER = 2.dp

private val ROW_BAR_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .height(BAR_HEIGHT)

private val ROW_BAR_HORIZONTAL_ARRANGEMENT = Arrangement.SpaceBetween

private val EDGE_SPACER_MODIFIER = Modifier.width(EDGE_HORIZONTAL_SPACER)
private val INNER_SPACER_MODIFIER = Modifier.width(INNER_HORIZONTAL_SPACER)

private val INDICATOR_CONTAINER_BASE_MODIFIER = Modifier
    .size(INDICATOR_CONTAINER_SIZE)
    .clip(CircleShape)
    .border(INDICATOR_BORDER_WIDTH, INDICATOR_BORDER_COLOR, CircleShape)

@Composable
fun NicheBottomBar(
    niche: NichesInfo,
    currentSort: Order,
    onSortChange: (Order) -> Unit,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = ROW_BAR_BASE_MODIFIER,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = ROW_BAR_HORIZONTAL_ARRANGEMENT
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = EDGE_SPACER_MODIFIER)

                SortByOrder(
                    NICHE_SORT_ORDERS,
                    currentSort,
                    onSelect = onSortChange,
                    containerColor = Theme.tabLevel0,
                    circle = true
                )

                Spacer(modifier = INNER_SPACER_MODIFIER)
            }
            Spacer(modifier = INNER_SPACER_MODIFIER)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = INDICATOR_CONTAINER_BASE_MODIFIER.background(Theme.tabLevel0),
                    contentAlignment = Alignment.Center
                ) {
                    TabBarPoints(columns, true)
                }

                Spacer(modifier = EDGE_SPACER_MODIFIER)
            }
        }
    }
}

@Preview
@Composable
fun NicheBottomBarPreview() {
    XvideosTheme {
        NicheBottomBar(
            niche = NichesInfo(id = "id", name = "Sample Niche", thumbnail = ""),
            currentSort = Order.TRENDING,
            onSortChange = {},
            columns = 2,
        )
    }
}
