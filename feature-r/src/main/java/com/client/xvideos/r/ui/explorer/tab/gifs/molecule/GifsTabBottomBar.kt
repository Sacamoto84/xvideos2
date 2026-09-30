package com.client.xvideos.r.ui.explorer.tab.gifs.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.ui.atom.ButtonUp
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder
import kotlinx.collections.immutable.persistentListOf

private val DEFAULT_ORDERS = persistentListOf(Order.TOP_WEEK, Order.TOP_MONTH, Order.TOP, Order.TRENDING, Order.LATEST)
private val SEARCH_ORDERS = persistentListOf(
    Order.RELEVANT, Order.TOP, Order.TOP_WEEK,
    Order.TOP_MONTH, Order.TRENDING, Order.LATEST
)

@Composable
fun GifsTabBottomBar(
    searchField: @Composable (Modifier) -> Unit,
    searchQuery: String,
    isFocused: Boolean,
    sortType: Order,
    onSortSelect: (Order) -> Unit,
    onUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(Theme.tabLevel1)) {
        HorizontalDivider(color = Theme.R.colorBorderGray)
        Row(
            modifier = Modifier
                .padding(top = 1.dp, start = 1.dp)
                .background(Theme.tabLevel1),
            verticalAlignment = Alignment.Bottom
        ) {
            AnimatedVisibility(
                visible = !isFocused,
                enter = expandHorizontally(animationSpec = tween(250)) + fadeIn(tween(250)),
                exit = shrinkHorizontally(animationSpec = tween(250)) + fadeOut(tween(250)),
            ) {
                val orders = if (searchQuery.isEmpty()) DEFAULT_ORDERS else SEARCH_ORDERS
                SortByOrder(
                    containerColor = Theme.tabLevel0,
                    list = orders,
                    selected = sortType,
                    onSelect = onSortSelect
                )
            }

            searchField(
                Modifier
                    .padding(start = 4.dp)
                    .weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            AnimatedVisibility(
                visible = !isFocused,
                enter = expandHorizontally(animationSpec = tween(250), expandFrom = Alignment.Start) + fadeIn(tween(250)),
                exit = shrinkHorizontally(animationSpec = tween(250), shrinkTowards = Alignment.Start) + fadeOut(tween(250)),
            ) {
                ButtonUp(onClick = onUpClick)
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        HorizontalDivider(color = Theme.R.colorCommonBackground)
        HorizontalDivider(color = Theme.R.colorBorderGray)
    }
}

@Preview
@Composable
private fun GifsTabBottomBarPreview() {
    GifsTabBottomBar(
        searchField = { modifier ->
            Box(
                modifier
                    .height(40.dp)
                    .background(Color.DarkGray, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(" Search...", color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }
        },
        searchQuery = "",
        isFocused = false,
        sortType = Order.TRENDING,
        onSortSelect = {},
        onUpClick = {}
    )
}
