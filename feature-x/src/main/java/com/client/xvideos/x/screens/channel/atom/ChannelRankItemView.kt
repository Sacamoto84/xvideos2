package com.client.xvideos.x.screens.channel.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelRankItem

@Composable
fun ChannelRankItemView(
    item: ChannelRankItem,
    categoryLabel: String,
    onRankingClick: (targetUrl: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = item.geo,
            fontSize = 12.sp,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "# ${item.rank}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDE2600),
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .clickable {
                    onRankingClick(
                        item.link,
                        item.label.ifBlank { "$categoryLabel - ${item.geo} #${item.rank}" }
                    )
                }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Preview
@Composable
private fun ChannelRankItemViewPreview() {
    ChannelRankItemView(
        item = ChannelRankItem(
            rank = 42,
            geo = "Мировой",
            link = "/rankings/world",
            label = "Мировой рейтинг",
        ),
        categoryLabel = "Рейтинги моделей",
        onRankingClick = { _, _ -> }
    )
}
