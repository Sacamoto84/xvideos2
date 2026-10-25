package com.client.xvideos.xlive.ui.room.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.xlive.ui.XLiveColors

/**
 * Кнопка в шапке комнаты: сохранить в «Мои» или убрать оттуда. Сохранённая —
 * закрашенная закладка цвета раздела; что делает кнопка, говорит подсказка по
 * долгому нажатию.
 *
 * @param saved комната уже в «Моих».
 */
@Composable
fun XLiveSaveButton(
    saved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = if (saved) "Убрать из «Моих»" else "Сохранить в «Мои»"
    HintTooltip(text = hint, modifier = modifier) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(XLiveColors.Pill)
        ) {
            Icon(
                imageVector = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = hint,
                tint = if (saved) XLiveColors.Accent else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Preview
@Composable
private fun XLiveSaveButtonPreview() {
    XLiveSaveButton(saved = true, onClick = {})
}
