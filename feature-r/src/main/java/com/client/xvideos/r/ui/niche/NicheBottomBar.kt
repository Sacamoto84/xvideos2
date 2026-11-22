package com.client.xvideos.r.ui.niche

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import com.client.xvideos.r.model.NICHE_VIDEO_ORDERS
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.atom.TabBarPoints
import com.client.xvideos.r.ui.ui.sortByOrder.RSortOrderIcon
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun NicheBottomBar(
    niche: NichesInfo,
    currentSort: Order,
    onSortChange: (Order) -> Unit,
    columns: Int,
    modifier: Modifier = Modifier,
    scrollMode: @Composable () -> Unit = {},
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(2.dp))

                SortByOrder(
                    list = NICHE_VIDEO_ORDERS,
                    selected = currentSort,
                    onSelect = onSortChange,
                    containerColor = Theme.tabLevel0,
                    circle = true,
                    orderIcon = { RSortOrderIcon(it) },
                    showSelectedLabel = true,
                )

                Spacer(modifier = Modifier.width(4.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            scrollMode()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.DarkGray, CircleShape)
                        .background(Theme.tabLevel0),
                    contentAlignment = Alignment.Center
                ) {
                    TabBarPoints(columns, true)
                }

                Spacer(modifier = Modifier.width(2.dp))
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
            currentSort = Order.HOT,
            onSortChange = {},
            columns = 2,
        )
    }
}
