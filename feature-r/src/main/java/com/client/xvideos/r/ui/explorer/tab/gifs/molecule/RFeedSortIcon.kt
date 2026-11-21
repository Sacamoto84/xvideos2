package com.client.xvideos.r.ui.explorer.tab.gifs.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.client.xvideos.common.ui.atom.SortCalendarIcon
import com.client.xvideos.r.model.Order

@Composable
internal fun RFeedSortIcon(order: Order) {
    when (order) {
        Order.TOP_WEEK -> SortCalendarIcon(7)
        Order.TOP_MONTH, Order.TOP28 -> SortCalendarIcon(28)
        else -> Icon(
            imageVector = when (order) {
                Order.TOP -> Icons.Filled.EmojiEvents
                Order.TRENDING -> Icons.Filled.LocalFireDepartment
                Order.LATEST -> Icons.Filled.AutoAwesome
                Order.RELEVANT -> Icons.AutoMirrored.Filled.ManageSearch
                else -> Icons.AutoMirrored.Filled.Sort
            },
            contentDescription = null,
            tint = Color.White
        )
    }
}
