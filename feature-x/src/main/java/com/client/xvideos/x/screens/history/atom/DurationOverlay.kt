package com.client.xvideos.x.screens.history.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val historyDurationOffsetY = (-3).dp

/**
 * Плашка отображения длительности ролика с тенью.
 */
@Composable
fun DurationOverlay(duration: String) {
    val text = remember(duration) { duration.trim().removeSuffix(".") }
    if (text.isEmpty()) return
    Box(modifier = Modifier) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .offset(1.dp, historyDurationOffsetY + 1.dp),
            textAlign = TextAlign.Right,
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .offset(0.dp, historyDurationOffsetY),
            textAlign = TextAlign.Right,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

@Preview
@Composable
private fun DurationOverlayPreview() {
    DurationOverlay("12:34")
}
