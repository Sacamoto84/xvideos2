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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.r.common.ThemeRed
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.ui.atom.ButtonUp
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun NichesBottomBar(
    isSearchFocused: Boolean,
    sortType: Order,
    onSortTypeChange: (Order) -> Unit,
    onUpClick: () -> Unit,
    searchWidget: @Composable (Modifier) -> Unit
) {

    val haptic = LocalHapticFeedback.current

    Column(Modifier.background(ThemeRed.colorTabLevel1)) {
        HorizontalDivider(color = ThemeRed.colorBorderGray)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).background(ThemeRed.colorTabLevel1),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom
        )
        {
            AnimatedVisibility(visible = !isSearchFocused) {
                SortByOrder( list = listOf( Order.NICHES_SUBSCRIBERS_D, Order.NICHES_SUBSCRIBERS_A, Order.NICHES_POST_D, Order.NICHES_POST_A, Order.NICHES_NAME_A_Z, Order.NICHES_NAME_Z_A ),
                    selected = sortType, onSelect = onSortTypeChange, containerColor = ThemeRed.colorTabLevel0 )
            }

            searchWidget(Modifier.padding(horizontal = 4.dp).weight(1f))

            AnimatedVisibility(visible = !isSearchFocused) {
                ButtonUp {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onUpClick()
                }
            }
        }
        HorizontalDivider(color = ThemeRed.colorBorderGray)
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