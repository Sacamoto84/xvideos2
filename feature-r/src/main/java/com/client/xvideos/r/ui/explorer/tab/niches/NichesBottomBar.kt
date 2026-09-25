package com.client.xvideos.r.ui.explorer.tab.niches

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.ui.atom.ButtonUp
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.collections.immutable.persistentListOf

private val BAR_VERTICAL_PADDING = 2.dp
private val SEARCH_HORIZONTAL_PADDING = 4.dp

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
    onUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    searchWidget: @Composable (Modifier) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val handleUpClick = remember(haptic, onUpClick) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onUpClick()
        }
    }

    Column(modifier = modifier.background(Theme.tabLevel1)) {
        HorizontalDivider(color = Theme.R.colorBorderGray)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = BAR_VERTICAL_PADDING),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            AnimatedVisibility(visible = !isSearchFocused) {
                SortByOrder(
                    list = NICHE_SORT_ORDERS,
                    selected = sortType,
                    onSelect = onSortTypeChange,
                    containerColor = Theme.tabLevel0
                )
            }

            searchWidget(Modifier.padding(horizontal = SEARCH_HORIZONTAL_PADDING).weight(1f))

            AnimatedVisibility(visible = !isSearchFocused) {
                ButtonUp(onClick = handleUpClick)
            }
        }
        HorizontalDivider(color = Theme.R.colorBorderGray)
    }
}

@Preview
@Composable
fun NichesBottomBarPreview() {
    XvideosTheme {
        NichesBottomBar(
            isSearchFocused = false,
            sortType = Order.NICHES_SUBSCRIBERS_D,
            onSortTypeChange = {},
            onUpClick = {},
            searchWidget = {}
        )
    }
}
