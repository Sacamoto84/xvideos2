package com.client.xvideos.x.screens.search.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.x.search.model.Channel

@Composable
fun ChannelSuggestionItem(
    channel: Channel,
    onSelect: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect(channel) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UrlImage(
            url = channel.avatarUrl,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF333333))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.isVerified) {
                    Text(
                        text = " ✓",
                        color = Color(0xFFFF9900),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            if (channel.hasSubscribers) {
                Text(
                    text = "${channel.subscribers} подписчиков",
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChannelSuggestionItemPreview() {
    ChannelSuggestionItem(
        channel = Channel(
            N = "Sample Channel",
            F = "/channels/sample",
            T = "channel",
            CPV = true,
            M = 0,
            L = 0,
            P = "",
            RF = "50K"
        ),
        onSelect = {}
    )
}
