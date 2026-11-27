package com.client.xvideos.r.ui.explorer.tab.niches.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.ui.sortByOrder.RSortOrderIcon
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder
import kotlinx.collections.immutable.persistentListOf

private val NICHE_SORT_ORDERS = persistentListOf(
    Order.NICHES_SUBSCRIBERS_D,
    Order.NICHES_SUBSCRIBERS_A,
    Order.NICHES_POST_D,
    Order.NICHES_POST_A,
    Order.NICHES_NAME_A_Z,
    Order.NICHES_NAME_Z_A
)

@Composable
fun NichesBottomBar(
    isSearchFocused: Boolean,
    sortType: Order,
    onSortTypeChange: (Order) -> Unit,
    modifier: Modifier = Modifier,
    searchWidget: @Composable (Modifier) -> Unit
) {
    Column(modifier = modifier.background(Theme.tabLevel1)) {
        HorizontalDivider(color = Theme.R.colorBorderGray)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            AnimatedVisibility(visible = !isSearchFocused) {
                SortByOrder(
                    list = NICHE_SORT_ORDERS,
                    selected = sortType,
                    onSelect = onSortTypeChange,
                    containerColor = Theme.tabLevel0,
                    orderIcon = { RSortOrderIcon(it) },
                )
            }

            searchWidget(Modifier.weight(1f).padding(start = 4.dp))
        }
        HorizontalDivider(color = Theme.R.colorBorderGray)
    }
}

@Preview
@Composable
private fun NichesBottomBarPreview() {
    NichesBottomBar(
        isSearchFocused = false,
        sortType = Order.NICHES_SUBSCRIBERS_D,
        onSortTypeChange = {},
        searchWidget = {}
    )
}
