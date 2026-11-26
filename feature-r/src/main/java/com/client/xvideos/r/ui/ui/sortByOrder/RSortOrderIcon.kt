package com.client.xvideos.r.ui.ui.sortByOrder

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
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
        Order.TOP,
        Order.HOT,
        Order.TRENDING,
        Order.LATEST,
        Order.OLDEST,
        Order.RELEVANT -> Icon(
            imageVector = order.sortVector(),
            contentDescription = null,
            tint = Color.White,
        )
    }
}

private fun Order.sortVector(): ImageVector = when (this) {
    Order.TOP -> Icons.Filled.EmojiEvents
    Order.HOT, Order.TRENDING -> Icons.Filled.LocalFireDepartment
    Order.LATEST -> Icons.Filled.AutoAwesome
    Order.OLDEST -> Icons.Filled.History
    Order.RELEVANT -> Icons.AutoMirrored.Filled.ManageSearch
    else -> error("Unsupported non-vector order: $this")
}

@Preview
@Composable
private fun RSortOrderIconPreview() {
    RSortOrderIcon(order = Order.TRENDING)
}
