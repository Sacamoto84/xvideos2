package com.client.xvideos.r.ui.ui.sortByOrder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.r.model.Order

@Composable
internal fun SortOrderOptionLabel(
    order: Order,
    selected: Boolean,
    withIcon: Boolean,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    if (withIcon) {
        BasicText(
            order.toIconLabel(),
            modifier = modifier,
            style = textStyle.copy(
                color = if (selected) Color(0xFFFF6A87) else Color.White,
                fontSize = 14.sp
            )
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Color(0xFF222222) else Color.Transparent),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicText(
                order.toDisplayName(),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = textStyle,
                autoSize = SORT_ORDER_TEXT_AUTO_SIZE,
                maxLines = 1
            )
        }
    }
}

@Preview
@Composable
private fun SortOrderOptionLabelPreview() {
    SortOrderOptionLabel(
        order = Order.TOP,
        selected = true,
        withIcon = false,
        textStyle = TextStyle(color = Color.White),
    )
}
