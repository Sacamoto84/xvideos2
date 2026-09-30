package com.client.xvideos.haptic.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HapticTopBar(
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1B1B1B))
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
    ) {
        Text(
            text = "Haptic Feedback — демо",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Text(
            text = "Нажми кнопку, чтобы почувствовать отклик. Доступно $totalItems типов.",
            color = Color(0xFFB0B0B0),
            fontSize = 13.sp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
        )
    }
}

@Preview
@Composable
private fun HapticTopBarPreview() {
    HapticTopBar(totalItems = 13)
}
