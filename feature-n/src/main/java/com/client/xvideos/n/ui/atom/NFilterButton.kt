package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip

/**
 * Кнопка вызова фильтров каталога манги (аналог FilterButton из раздела L).
 */
@Composable
fun NFilterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasActiveFilter: Boolean = false
) {
    val hint = "Фильтры"
    HintTooltip(text = hint, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = 2.dp,
                    color = if (hasActiveFilter) Color(0xFFED2553) else Color(0xFF434343),
                    shape = RoundedCornerShape(4.dp)
                )
                .background(Color(0xFF414141))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = hint,
                tint = if (hasActiveFilter) Color(0xFFED2553) else Color.White
            )
        }
    }
}

@Preview
@Composable
private fun NFilterButtonPreview() {
    NFilterButton(
        onClick = {},
        hasActiveFilter = true
    )
}
