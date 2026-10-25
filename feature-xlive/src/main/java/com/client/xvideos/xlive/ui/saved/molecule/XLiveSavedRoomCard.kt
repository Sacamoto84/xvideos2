package com.client.xvideos.xlive.ui.saved.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.xlive.model.XLiveSavedRoom
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.room.atom.XLiveLiveBadge
import com.client.xvideos.xlive.ui.saved.atom.XLiveOfflineBadge

private const val THUMBNAIL_ASPECT_RATIO = 16f / 9f
private const val OFFLINE_ALPHA = 0.45f
private val CardShape = RoundedCornerShape(8.dp)

/**
 * Карточка сохранённой комнаты — вид карточки «Эфиров», но вместо зрителей
 * плашка эфира: в «Моих» и «Истории» зрителей не знаем, а главное — идёт ли
 * эфир. Снимок комнаты не в эфире приглушён.
 *
 * @param thumbnail снимок эфира по нику.
 * @param online в эфире; `null` — ещё не проверено, плашки нет.
 * @param onLongClick долгое нажатие — убрать из списка.
 */
@Composable
fun XLiveSavedRoomCard(
    room: XLiveSavedRoom,
    thumbnail: String,
    online: Boolean?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(CardShape)
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF26262C), CardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                .background(Color(0xFF1F1F24))
        ) {
            UrlImage(
                url = thumbnail,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (online == false) OFFLINE_ALPHA else 1f)
            )
            val badgeModifier = Modifier
                .align(Alignment.BottomStart)
                .padding(5.dp)
            when (online) {
                true -> XLiveLiveBadge(modifier = badgeModifier)
                false -> XLiveOfflineBadge(modifier = badgeModifier)
                null -> Unit
            }
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = room.username,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = room.age?.toString().orEmpty(),
                fontSize = 11.sp,
                color = XLiveColors.Label,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun XLiveSavedRoomCardPreview() {
    XLiveSavedRoomCard(
        room = XLiveSavedRoom(username = "room_a", age = 24),
        thumbnail = "",
        online = true,
        onClick = {},
        onLongClick = {},
    )
}
