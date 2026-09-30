package com.client.xvideos.x.screens.dashboards.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShadowedDurationText(
    durationText: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = durationText,
            modifier = Modifier
                .fillMaxWidth()
                .offset(0.5.dp, (-2.5).dp),
            textAlign = TextAlign.Right,
            fontSize = 14.sp,
            color = Color.Black
        )

        Text(
            text = durationText,
            modifier = Modifier
                .fillMaxWidth()
                .offset(0.dp, (-3).dp),
            textAlign = TextAlign.Right,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222222)
@Composable
private fun ShadowedDurationTextPreview() {
    ShadowedDurationText(durationText = "12:34")
}
