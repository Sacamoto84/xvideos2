package com.client.xvideos.x.screens.favorites.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Продолжительность видео в правом верхнем углу с «тенью» (как в оригинале). */
@Composable
fun DurationOverlay(
    duration: String,
    modifier: Modifier = Modifier,
) {
    val text = remember(duration) { duration.trim().removeSuffix(".") }
    if (text.isEmpty()) return
    Box(modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .offset(1.dp, (-2).dp),
            textAlign = TextAlign.Right,
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .offset(0.dp, (-3).dp),
            textAlign = TextAlign.Right,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun DurationOverlayPreview() {
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 32.dp)
            .background(Color(0xFF3A3A3A))
    ) {
        DurationOverlay("12:34")
    }
}
