package com.client.xvideos.x.screens.saved.atom

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
 * Подпись на превью карточки «Сохранённого» — размер или длительность:
 * белый текст с чёрной тенью, сдвинутой вправо-вниз, как длительность в
 * «Избранном» и ленте. Пустой текст не рисуется.
 */
@Composable
fun SavedOverlayText(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (text.isEmpty()) return
    Box(modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier.offset(1.dp, 1.dp),
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
private fun SavedOverlayTextPreview() {
    SavedOverlayText(
        text = "184.4 MB",
        modifier = Modifier
            .background(Color(0xFF3A3A3A))
            .padding(4.dp)
    )
}
