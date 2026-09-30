package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.screens.channel.atom.StatPill

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChannelHeaderStatsRow(
    header: ChannelHeaderModel,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-10).dp)
    ) {
        if (header.hasSubscribers) {
            StatPill(text = "${header.subscribers} подписчиков")
        }
        if (header.hasTotalViews) {
            StatPill(text = "${header.totalViews} просмотров")
        }
        if (header.videoCount > 0) {
            StatPill(text = "${header.videoCount} видео")
        }
    }
}

@Preview
@Composable
private fun ChannelHeaderStatsRowPreview() {
    ChannelHeaderStatsRow(
        header = ChannelHeaderModel(
            subscribers = "150.5K",
            totalViews = "25.4M",
            videoCount = 128
        )
    )
}
