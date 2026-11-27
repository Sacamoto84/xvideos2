package com.client.xvideos.n.ui.screens.explorer.tab.list.atom

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.ui.atom.SortCalendarIcon
import com.client.xvideos.n.model.NSortType

@Composable
internal fun NFeedSortIcon(sort: NSortType) {
    when (sort) {
        NSortType.RECENT -> Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White)
        NSortType.POPULAR_ALL_TIME ->
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color.White)
        NSortType.POPULAR_MONTH -> SortCalendarIcon(30)
        NSortType.POPULAR_WEEK -> SortCalendarIcon(7)
        NSortType.POPULAR_TODAY -> SortCalendarIcon(1)
    }
}

@Preview
@Composable
private fun NFeedSortIconPreview() {
    NFeedSortIcon(sort = NSortType.POPULAR_WEEK)
}
