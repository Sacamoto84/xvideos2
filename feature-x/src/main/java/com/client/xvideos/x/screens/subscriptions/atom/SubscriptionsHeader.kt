package com.client.xvideos.x.screens.subscriptions.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.ui.atom.SavedTabHeader

/**
 * Заголовок вкладки подписок со счётчиком — та же шапка, что у «Избранного»
 * и «Сохранённого», счётчик справа. [actions] — после счётчика (сортировка).
 */
@Composable
fun SubscriptionsHeader(
    title: String,
    count: Int,
    isModel: Boolean,
    modifier: Modifier = Modifier,
    topCutout: Dp = 0.dp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    SavedTabHeader(title = title, topCutout = topCutout, modifier = modifier) {
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
        actions()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun SubscriptionsHeaderPreview() {
    SubscriptionsHeader(
        title = "Каналы",
        count = 42,
        isModel = false
    )
}
