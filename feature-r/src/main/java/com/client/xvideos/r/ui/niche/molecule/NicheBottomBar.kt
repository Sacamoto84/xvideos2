package com.client.xvideos.r.ui.niche.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.NICHE_VIDEO_ORDERS
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.ui.sortByOrder.RSortOrderIcon
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder

@Composable
fun NicheBottomBar(
    currentSort: Order,
    onSortChange: (Order) -> Unit,
    modifier: Modifier = Modifier,
    scrollMode: @Composable () -> Unit = {},
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    circle = false,
                    orderIcon = { RSortOrderIcon(it) },
                    showSelectedLabel = true,
                )

                Spacer(modifier = Modifier.width(4.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            scrollMode()
        }
    }
}

@Preview
@Composable
private fun NicheBottomBarPreview() {
    NicheBottomBar(
        currentSort = Order.HOT,
        onSortChange = {},
    )
}
