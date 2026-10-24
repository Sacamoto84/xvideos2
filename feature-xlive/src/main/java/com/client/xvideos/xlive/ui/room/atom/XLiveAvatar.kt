package com.client.xvideos.xlive.ui.room.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.client.xvideos.xlive.ui.XLiveColors

/**
 * Аватар модели в шапке комнаты: круг с белой рамкой, как у канала X.
 *
 * Картинки для аватара сайт не отдаёт, поэтому внутри первая буква ника.
 */
@Composable
fun XLiveAvatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(68.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .background(XLiveColors.AvatarBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            color = XLiveColors.Accent,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
private fun XLiveAvatarPreview() {
    XLiveAvatar(name = "room_a")
}
