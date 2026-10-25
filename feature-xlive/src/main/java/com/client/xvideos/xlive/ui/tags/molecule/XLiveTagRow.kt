package com.client.xvideos.xlive.ui.tags.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
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
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.xlive.model.XLiveTag
import com.client.xvideos.xlive.model.XLiveTagRoom
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.room.atom.XLiveStatPill
import com.client.xvideos.xlive.ui.tags.atom.XLiveTagThumb

private val RowShape = RoundedCornerShape(8.dp)

/**
 * Строка таблицы тегов: тег со счётчиками зрителей и комнат, под ним лента
 * превью его популярных комнат. Тап по шапке — комнаты с тегом, по превью —
 * сама комната.
 */
@Composable
fun XLiveTagRow(
    tag: XLiveTag,
    onOpenTag: () -> Unit,
    onOpenRoom: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RowShape)
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF26262C), RowShape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenTag)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Тег и счётчики — в своём ряду на всю ширину, стрелка — у правого края.
            // Длинный тег сжимается многоточием, счётчики не уезжают.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "#${tag.name}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                XLiveStatPill(Icons.Outlined.Visibility, "Зрители", tag.viewers.toLong().toPrettyCount())
                XLiveStatPill(Icons.Outlined.Videocam, "Комнаты", tag.rooms.toLong().toPrettyCount())
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = XLiveColors.Label,
                modifier = Modifier.size(20.dp)
            )
        }
        if (tag.topRooms.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(items = tag.topRooms, key = { it.username }) { room ->
                    XLiveTagThumb(room = room, onClick = { onOpenRoom(room.username) })
                }
            }
        }
    }
}

@Preview
@Composable
private fun XLiveTagRowPreview() {
    XLiveTagRow(
        tag = XLiveTag(
            name = "petite",
            viewers = 249_367,
            rooms = 1195,
            topRooms = listOf(XLiveTagRoom("room_a", ""), XLiveTagRoom("room_b", "")),
        ),
        onOpenTag = {},
        onOpenRoom = {},
    )
}
