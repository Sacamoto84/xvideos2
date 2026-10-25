package com.client.xvideos.xlive.ui.saved.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Серая плашка «Не в эфире» на снимке сохранённой комнаты — пара к красной «В эфире». */
@Composable
fun XLiveOfflineBadge(modifier: Modifier = Modifier) {
    Text(
        text = "НЕ В ЭФИРЕ",
        color = Color(0xFFCCCCCC),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Color(0xCC26262B), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Preview
@Composable
private fun XLiveOfflineBadgePreview() {
    XLiveOfflineBadge()
}
