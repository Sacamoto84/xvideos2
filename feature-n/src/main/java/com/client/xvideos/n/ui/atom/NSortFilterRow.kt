package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NSortType

/**
 * Строка переключения фильтра популярности (Все время, Неделя, День).
 */
@Composable
fun NSortFilterRow(
    selectedSort: NSortType,
    onSortSelected: (NSortType) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp
) {
    val sortOptions = listOf(
        NSortType.POPULAR_ALL_TIME,
        NSortType.POPULAR_MONTH,
        NSortType.POPULAR_WEEK,
        NSortType.POPULAR_TODAY
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.tabLevel0)
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sortOptions.forEach { sort ->
                val isSelected = sort == selectedSort
                val chipShape = RoundedCornerShape(16.dp)
                val backgroundColor = if (isSelected) Color(0xFFED2553) else Theme.tabLevel2
                val textColor = if (isSelected) Color.White else Color(0xFFCCCCCC)

                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(backgroundColor)
                        .clickable { onSortSelected(sort) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = sort.title,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NSortFilterRowPreview() {
    NSortFilterRow(
        selectedSort = NSortType.POPULAR_ALL_TIME,
        onSortSelected = {},
        topInset = 24.dp
    )
}
