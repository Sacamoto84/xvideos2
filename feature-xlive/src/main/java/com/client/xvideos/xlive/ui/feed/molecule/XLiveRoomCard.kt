package com.client.xvideos.xlive.ui.feed.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.xlive.model.XLiveRoomItem
import com.client.xvideos.xlive.model.flagEmoji
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.feed.atom.XLiveViewersBadge

private const val THUMBNAIL_ASPECT_RATIO = 16f / 9f
private val CardShape = RoundedCornerShape(8.dp)

/**
 * Карточка комнаты в сетке «Эфиры» — вид карточек сетки X: снимок эфира с
 * затемнением снизу, флаг страны в углу, зрители, под снимком ник, возраст и
 * первый тег.
 */
@Composable
fun XLiveRoomCard(
    item: XLiveRoomItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flag = flagEmoji(item.country)
    val meta = listOfNotNull(item.age?.toString(), item.tags.firstOrNull()?.let { "#$it" }).joinToString(" · ")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(CardShape)
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF26262C), CardShape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                .background(Color(0xFF1F1F24))
        ) {
            if (item.thumbnail.isNotBlank()) {
                UrlImage(url = item.thumbnail, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000))))
            )
            if (flag.isNotEmpty()) {
                Text(
                    text = flag,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 5.dp, top = 2.dp)
                )
            }
            XLiveViewersBadge(
                viewers = item.viewers,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
            )
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = item.username,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    fontSize = 11.sp,
                    color = XLiveColors.Label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
private fun XLiveRoomCardPreview() {
    XLiveRoomCard(
        item = XLiveRoomItem(username = "room_a", age = 21, country = "US", viewers = 63_200, tags = listOf("new")),
        onClick = {},
    )
}
