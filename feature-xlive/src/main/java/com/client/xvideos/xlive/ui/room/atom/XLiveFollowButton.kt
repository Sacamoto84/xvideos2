package com.client.xvideos.xlive.ui.room.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.xlive.ui.XLiveColors

private const val BUSY_ALPHA = 0.4f

/**
 * Кнопка в шапке комнаты: подписаться на сайте или отписаться. Подписка —
 * закрашенное сердце цвета раздела; без входа кнопка ведёт на страницу входа.
 * Что делает кнопка, говорит подсказка по долгому нажатию.
 *
 * @param signedIn есть вход на сайт.
 * @param following аккаунт подписан на комнату.
 * @param busy запрос в пути: значок бледнеет, нажатие не в счёт.
 */
@Composable
fun XLiveFollowButton(
    signedIn: Boolean,
    following: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = when {
        !signedIn -> "Войти на сайт, чтобы подписаться"
        following -> "Отписаться на сайте"
        else -> "Подписаться на сайте"
    }
    HintTooltip(text = hint, modifier = modifier) {
        IconButton(
            onClick = onClick,
            enabled = !busy,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(XLiveColors.Pill)
        ) {
            Icon(
                imageVector = if (following) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = hint,
                tint = if (following) XLiveColors.Accent else Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .alpha(if (busy) BUSY_ALPHA else 1f)
            )
        }
    }
}

@Preview
@Composable
private fun XLiveFollowButtonPreview() {
    XLiveFollowButton(signedIn = true, following = true, busy = false, onClick = {})
}
