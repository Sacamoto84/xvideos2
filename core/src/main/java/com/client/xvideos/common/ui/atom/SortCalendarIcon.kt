package com.client.xvideos.common.ui.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SortCalendarIcon(days: Int) {
    // Число масштабируется вместе с пиктограммой. Название режима доступно отдельно.
    val digitSize = with(LocalDensity.current) { 9.dp.toSp() }
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color.White)
        Text(
            text = days.toString(),
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

@Preview
@Composable
private fun SortCalendarIconPreview() {
    SortCalendarIcon(days = 7)
}
