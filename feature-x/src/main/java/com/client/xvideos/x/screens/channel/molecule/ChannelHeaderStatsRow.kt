package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.core.R
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.screens.channel.atom.StatPill
import com.client.xvideos.x.screens.channel.model.channelStatCount

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChannelHeaderStatsRow(
    header: ChannelHeaderModel,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        // Ряд кнопок над плашками поднят на 12dp: без смещения между ними 12dp,
        // а нижний отступ держит прежнее расстояние до карточки «О канале».
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        if (header.hasSubscribers) {
            val subscribers = remember(header.subscribers) { channelStatCount(header.subscribers) }
            StatPill(icon = painterResource(R.drawable.members), contentDescription = "Подписчики", text = subscribers)
        }
        if (header.hasTotalViews) {
            val views = remember(header.totalViews) { channelStatCount(header.totalViews) }
            StatPill(
                icon = rememberVectorPainter(Icons.Outlined.Visibility),
                contentDescription = "Просмотры",
                text = views
            )
        }
        if (header.videoCount > 0) {
            StatPill(icon = painterResource(R.drawable.posts), contentDescription = "Видео", text = "${header.videoCount}")
        }
    }
}

@Preview
@Composable
private fun ChannelHeaderStatsRowPreview() {
    ChannelHeaderStatsRow(
        header = ChannelHeaderModel(
            subscribers = "715,9 к",
            totalViews = "720 776 005",
            videoCount = 1248
        )
    )
}
