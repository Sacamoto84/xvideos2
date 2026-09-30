package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelRankGroup
import com.client.xvideos.x.model.ChannelRankItem
import com.client.xvideos.x.model.ChannelRankingCategory
import com.client.xvideos.x.screens.channel.atom.ChannelRankItemView

/**
 * Блок отображения рейтингов модели или канала («Рейтинги порноактрис», «Глобальные рейтинги»).
 */
@Composable
fun ChannelRankingsSection(
    rankings: List<ChannelRankingCategory>,
    onRankingClick: (targetUrl: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1719))
            .border(1.dp, Color(0x33DE2600), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rankings.forEachIndexed { index, category ->
            if (index > 0) {
                HorizontalDivider(
                    color = Color(0x22FFFFFF),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${category.label}:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                category.ranks.forEach { group ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${group.label}:",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    group.ranks.forEach { item ->
                        ChannelRankItemView(
                            item = item,
                            categoryLabel = category.label,
                            onRankingClick = onRankingClick
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChannelRankingsSectionPreview() {
    ChannelRankingsSection(
        rankings = listOf(
            ChannelRankingCategory(
                label = "Рейтинги моделей",
                ranks = listOf(
                    ChannelRankGroup(
                        label = "По всему миру",
                        ranks = listOf(
                            ChannelRankItem(rank = 42, geo = "Мировой", link = "/rankings/world", label = "Мировой рейтинг")
                        )
                    )
                )
            )
        ),
        onRankingClick = { _, _ -> }
    )
}
