package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelSortOrder

/**
 * Панель выбора сортировки роликов канала («Свежие», «Новые», «Топ»).
 *
 * @param selectedSort Текущий выбранный режим [ChannelSortOrder].
 * @param onSortChange Колбэк переключения сортировки.
 * @param modifier Модификатор макета.
 */
@Composable
fun ChannelSortBar(
    selectedSort: ChannelSortOrder,
    onSortChange: (ChannelSortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorts = remember { ChannelSortOrder.entries }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF040404))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (sort in sorts) {
            val isSelected = sort == selectedSort
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFDE2600) else Color(0xFF1E1E22),
                animationSpec = tween(durationMillis = 180),
                label = "sort_chip_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color(0xFFAAAAAA),
                animationSpec = tween(durationMillis = 180),
                label = "sort_chip_text"
            )

            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .clickable { onSortChange(sort) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sort.title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChannelSortBarPreview() {
    ChannelSortBar(
        selectedSort = ChannelSortOrder.NEW,
        onSortChange = {}
    )
}
