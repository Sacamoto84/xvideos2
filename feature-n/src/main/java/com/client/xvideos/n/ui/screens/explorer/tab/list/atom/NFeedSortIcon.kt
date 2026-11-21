package com.client.xvideos.n.ui.screens.explorer.tab.list.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.client.xvideos.n.model.NSortType

@Composable
internal fun NFeedSortIcon(sort: NSortType) {
    when (sort) {
        NSortType.RECENT -> Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White)
        NSortType.POPULAR_ALL_TIME ->
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color.White)
        NSortType.POPULAR_MONTH -> SortCalendarIcon("30")
        NSortType.POPULAR_WEEK -> SortCalendarIcon("7")
        NSortType.POPULAR_TODAY -> SortCalendarIcon("1")
    }
}

@Composable
private fun SortCalendarIcon(days: String) {
    // Число масштабируется вместе с пиктограммой. Название режима доступно отдельно.
    val digitSize = with(LocalDensity.current) { 9.dp.toSp() }
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color.White)
        Text(
            text = days,
            modifier = Modifier.offset(y = 3.dp).clearAndSetSemantics { },
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontSize = digitSize,
            fontWeight = FontWeight.Bold,
            lineHeight = digitSize,
            maxLines = 1
        )
    }
}
