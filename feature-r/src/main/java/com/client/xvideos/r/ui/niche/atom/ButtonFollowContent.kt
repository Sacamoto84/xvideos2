package com.client.xvideos.r.ui.niche.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Кнопка подписки/отписки в профиле ниши.
 */
@Composable
fun ButtonFollowContent(
    isFollowed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonText = if (isFollowed) "Выйти" else "Подписаться"
    val buttonTextColor = if (isFollowed) Color.White else Color.Black
    val buttonBgColor = if (isFollowed) Theme.tabLevel1 else Theme.R.colorYellow
    val buttonShape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .padding(end = 4.dp)
            .clip(buttonShape)
            .width(128.dp)
            .height(44.dp)
            .then(
                if (isFollowed) {
                    Modifier.border(1.dp, Color.White, buttonShape)
                } else {
                    Modifier.border(1.dp, Color.Transparent, buttonShape)
                }
            )
            .background(buttonBgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buttonText,
            color = buttonTextColor
        )
    }
}

@Preview
@Composable
private fun ButtonFollowContentPreview() {
    XvideosTheme {
        ButtonFollowContent(
            isFollowed = false,
            onClick = {}
        )
    }
}
