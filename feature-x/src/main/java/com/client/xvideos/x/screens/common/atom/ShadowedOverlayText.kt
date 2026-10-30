package com.client.xvideos.x.screens.common.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Подпись на превью карточки («Избранное», «Сохранённое», «История») —
 * длительность или размер: белый текст с чёрной тенью, сдвинутой на 0.5dp
 * вправо-вниз. Без собственных сдвигов по вертикали: место задаёт экран
 * через `align` и `padding`, поэтому на всех трёх экранах подпись стоит на
 * одной высоте от нижнего края. Пустой текст не рисуется.
 */
@Composable
fun ShadowedOverlayText(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isEmpty()) return
    Box(modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier.offset(0.5.dp, 0.5.dp),
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

@Preview
@Composable
private fun ShadowedOverlayTextPreview() {
    ShadowedOverlayText(
        text = "12:34",
        modifier = Modifier
            .background(Color(0xFF3A3A3A))
            .padding(4.dp)
    )
}
