package com.client.xvideos.xlive.ui.room.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.xlive.model.XLiveTopic
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.room.atom.XLiveTagChip

/** Тема комнаты: текст и под ним теги чипами, как теги плеера X. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XLiveRoomTopic(topic: XLiveTopic, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (topic.text.isNotBlank()) {
            Text(
                text = topic.text,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = XLiveColors.Value,
            )
        }
        if (topic.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                topic.tags.forEach { tag -> XLiveTagChip(text = tag) }
            }
        }
    }
}

@Preview
@Composable
private fun XLiveRoomTopicPreview() {
    XLiveRoomTopic(topic = XLiveTopic(text = "goal: 100 tk", tags = listOf("petite", "ginger", "new")))
}
