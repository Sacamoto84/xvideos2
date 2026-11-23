package com.client.xvideos.r.ui.ui.sortByOrder

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.atom.SortCalendarIcon
import com.client.xvideos.core.R
import com.client.xvideos.r.model.Order

@Composable
internal fun RSortOrderIcon(order: Order) {
    when (order) {
        Order.TOP_WEEK -> SortCalendarIcon(7)
        Order.TOP_MONTH, Order.TOP28 -> SortCalendarIcon(28)
        Order.NICHES_SUBSCRIBERS_D, Order.NICHES_SUBSCRIBERS_A -> CatalogSortIcon(
            painterResource(R.drawable.members),
            ascending = order == Order.NICHES_SUBSCRIBERS_A,
        )
        Order.NICHES_POST_D, Order.NICHES_POST_A -> CatalogSortIcon(
            painterResource(R.drawable.posts),
            ascending = order == Order.NICHES_POST_A,
        )
        Order.NICHES_NAME_A_Z, Order.NICHES_NAME_Z_A -> CatalogSortIcon(
            rememberVectorPainter(Icons.Filled.SortByAlpha),
            ascending = order == Order.NICHES_NAME_A_Z,
        )
        else -> Icon(
            imageVector = when (order) {
                Order.TOP -> Icons.Filled.EmojiEvents
                Order.HOT, Order.TRENDING -> Icons.Filled.LocalFireDepartment
                Order.LATEST -> Icons.Filled.AutoAwesome
                Order.OLDEST -> Icons.Filled.History
                Order.RELEVANT -> Icons.AutoMirrored.Filled.ManageSearch
                else -> Icons.AutoMirrored.Filled.Sort
            },
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun CatalogSortIcon(painter: Painter, ascending: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White,
        )
        Icon(
            imageVector = if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = Color.White,
        )
    }
}
