package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.client.xvideos.x.screens.channel.model.channelRankingsTitle

/**
 * Сворачиваемый блок рейтингов модели или канала («Рейтинги порноактрис», «Глобальные рейтинги»).
 *
 * Свёрнутый — одна строка: заголовок ([channelRankingsTitle]) и стрелка. Нажатие
 * на строку вызывает [onToggleExpanded].
 */
@Composable
fun ChannelRankingsSection(
    rankings: List<ChannelRankingCategory>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRankingClick: (targetUrl: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Название единственной категории уже стоит в заголовке блока.
    val showCategoryLabels = rankings.size > 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1719))
            .border(1.dp, Color(0x33DE2600), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
        ) {
            Text(
                text = channelRankingsTitle(rankings),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Свернуть рейтинг" else "Показать рейтинг",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }

        if (isExpanded) {
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
                    if (showCategoryLabels) {
                        Text(
                            text = "${category.label}:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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
        isExpanded = true,
        onToggleExpanded = {},
        onRankingClick = { _, _ -> }
    )
}
