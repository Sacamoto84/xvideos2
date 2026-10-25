package com.client.xvideos.xlive.ui.room.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.room.XLiveFollowState
import com.client.xvideos.xlive.ui.room.atom.XLiveAvatar
import com.client.xvideos.xlive.ui.room.atom.XLiveFollowButton
import com.client.xvideos.xlive.ui.room.atom.XLiveLiveBadge
import com.client.xvideos.xlive.ui.room.atom.XLiveSaveButton
import com.client.xvideos.xlive.ui.room.atom.XLiveStatPill

/**
 * Шапка комнаты: аватар, ник, настоящее имя, плашка «В эфире», справа кнопки
 * «подписаться» и «в мои», под ними счётчики — раскладка шапки канала X.
 *
 * @param realName имя из анкеты; пустое — строки нет.
 * @param followers подписчики из анкеты; `null` — анкета ещё не пришла.
 * @param onAir сколько идёт эфир; `null` — неизвестно.
 * @param ratingPercent доля положительных оценок; `null` — оценок нет.
 * @param isSaved комната в «Моих».
 * @param onToggleSaved кнопка «в мои»: сохранить или убрать.
 * @param follow подписка на сайте.
 * @param onToggleFollow «подписаться» со входом: подписаться или отписаться.
 * @param onSignIn «подписаться» без входа: страница входа.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XLiveRoomHeader(
    name: String,
    realName: String,
    viewers: Int,
    followers: Int?,
    onAir: String?,
    ratingPercent: Int?,
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    follow: XLiveFollowState,
    onToggleFollow: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            XLiveAvatar(name = name)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (realName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = realName,
                        fontSize = 12.sp,
                        color = XLiveColors.Subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                XLiveLiveBadge()
            }
            Spacer(modifier = Modifier.width(8.dp))
            XLiveFollowButton(
                signedIn = follow.isSignedIn,
                following = follow.following,
                busy = follow.isBusy,
                onClick = if (follow.isSignedIn) onToggleFollow else onSignIn,
            )
            Spacer(modifier = Modifier.width(8.dp))
            XLiveSaveButton(saved = isSaved, onClick = onToggleSaved)
        }
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            XLiveStatPill(Icons.Outlined.Visibility, "Зрители", viewers.toLong().toPrettyCount())
            if (followers != null) {
                XLiveStatPill(Icons.Outlined.Group, "Подписчики", followers.toLong().toPrettyCount())
            }
            if (onAir != null) {
                XLiveStatPill(Icons.Outlined.Schedule, "В эфире", onAir)
            }
            if (ratingPercent != null) {
                XLiveStatPill(Icons.Outlined.ThumbUp, "Рейтинг", "$ratingPercent%")
            }
        }
    }
}

@Preview
@Composable
private fun XLiveRoomHeaderPreview() {
    XLiveRoomHeader(
        name = "room_a",
        realName = "Anna",
        viewers = 562,
        followers = 20332,
        onAir = "2 ч 05 мин",
        ratingPercent = 97,
        isSaved = false,
        onToggleSaved = {},
        follow = XLiveFollowState(isSignedIn = true, following = true),
        onToggleFollow = {},
        onSignIn = {},
    )
}
