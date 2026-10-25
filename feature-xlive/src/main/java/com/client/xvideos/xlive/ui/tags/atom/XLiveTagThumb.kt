package com.client.xvideos.xlive.ui.tags.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.xlive.model.XLiveTagRoom

private const val THUMB_ASPECT_RATIO = 16f / 9f

/**
 * Превью комнаты в строке тега: снимок эфира 16:9 и ник поверх затемнения.
 *
 * @param onClick открыть эту комнату.
 */
@Composable
fun XLiveTagThumb(
    room: XLiveTagRoom,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(128.dp)
            .aspectRatio(THUMB_ASPECT_RATIO)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1F1F24))
            .clickable(onClick = onClick)
    ) {
        if (room.thumbnail.isNotBlank()) {
            UrlImage(url = room.thumbnail, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Text(
            text = room.username,
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000))))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Preview
@Composable
private fun XLiveTagThumbPreview() {
    XLiveTagThumb(room = XLiveTagRoom(username = "room_a", thumbnail = ""), onClick = {})
}
