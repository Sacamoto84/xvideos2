package com.client.xvideos.x.screens.subscriptions.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Заголовок вкладки подписок со счётчиком.
 */
@Composable
fun SubscriptionsHeader(
    title: String,
    count: Int,
    isModel: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isModel) Color(0x33DE2600) else Color(0x331E88E5))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        color = if (isModel) Color(0xFFFF5252) else Color(0xFF42A5F5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        HorizontalDivider(color = Color(0xFF26262C), thickness = 0.5.dp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF16161C)
@Composable
private fun SubscriptionsHeaderPreview() {
    SubscriptionsHeader(
        title = "Каналы",
        count = 42,
        isModel = false
    )
}
